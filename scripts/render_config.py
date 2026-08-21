#!/usr/bin/env python3
"""
infra/services.yml (단일 소스) → 인프라 설정파일 렌더러.

생성물 (infra/generated/):
  - prometheus.yml
  - promtail-config.yaml
  - systemd/sh-platform-{name}.service
추가로 infra/nginx/sh-platform.conf 의 마커 영역에 서비스 location 블록을 주입한다.

사용법:
  python scripts/render_config.py            # 렌더링 + nginx 주입
  python scripts/render_config.py --check    # 생성물이 최신인지 검증만 (CI 드리프트 체크)

개념 문서: docs/guides/008-260821-infra-ssot-guide.md
"""

import argparse
import sys
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parent.parent
SERVICES_YML = ROOT / "infra" / "services.yml"
GENERATED_DIR = ROOT / "infra" / "generated"
NGINX_CONF = ROOT / "infra" / "nginx" / "sh-platform.conf"

NGINX_BEGIN = "# === [AUTO-GENERATED SERVICE BLOCKS — 수정 금지, render_config.py가 생성] ==="
NGINX_END = "# === [/AUTO-GENERATED SERVICE BLOCKS] ==="


def load_services():
    data = yaml.safe_load(SERVICES_YML.read_text(encoding="utf-8"))
    services = data["services"]

    ports = [s["port"] for s in services]
    if len(ports) != len(set(ports)):
        sys.exit(f"ERROR: 중복 포트 존재: {ports}")

    for s in services:
        routing = s.get("routing", "prefix")
        if routing not in ("prefix", "custom"):
            sys.exit(f"ERROR: {s['name']} routing은 prefix|custom 만 허용 (현재: {routing})")
        if routing == "prefix" and not s.get("prefix"):
            sys.exit(f"ERROR: {s['name']} 는 routing=prefix 인데 prefix 가 없음")

    return data


# ---------------------------------------------------------------------------
# prometheus.yml
# ---------------------------------------------------------------------------

def render_prometheus(data):
    app_targets = ", ".join(f'"localhost:{s["port"]}"' for s in data["services"])

    lines = [
        "# 이 파일은 자동 생성됩니다 — 수동 수정 금지",
        "# 원본: infra/services.yml  /  생성기: scripts/render_config.py",
        "",
        "global:",
        "  scrape_interval: 15s",
        "  evaluation_interval: 15s",
        "",
        "scrape_configs:",
        '  # Spring Boot (Micrometer)',
        '  - job_name: "spring-boot"',
        '    metrics_path: "/actuator/prometheus"',
        "    static_configs:",
        f"      - targets: [{app_targets}]",
        "",
    ]

    for t in data.get("static_targets", []):
        targets = ", ".join(f'"{x}"' for x in t["targets"])
        comment = f" # {t['comment']}" if t.get("comment") else ""
        lines += [
            f"  - job_name: \"{t['job']}\"{comment}",
            "    static_configs:",
            f"      - targets: [{targets}]",
            "",
        ]

    return "\n".join(lines).rstrip() + "\n"


# ---------------------------------------------------------------------------
# promtail-config.yaml
# ---------------------------------------------------------------------------

def render_promtail(data):
    app_home = data["app_home"]
    lines = [
        "# 이 파일은 자동 생성됩니다 — 수동 수정 금지",
        "# 원본: infra/services.yml  /  생성기: scripts/render_config.py",
        "",
        "server:",
        "  http_listen_port: 9080",
        "  grpc_listen_port: 0",
        "",
        "positions:",
        "  filename: /var/lib/promtail/positions.yaml",
        "",
        "clients:",
        "  - url: http://localhost:3100/loki/api/v1/push",
        "",
        "scrape_configs:",
    ]

    def emit_block(name, job, labels, path):
        lines.append(f"  - job_name: {name}")
        lines.append("    static_configs:")
        lines.append("      - targets:")
        lines.append("          - localhost")
        lines.append("        labels:")
        lines.append(f"          job: {job}")
        for k, v in labels.items():
            lines.append(f"          {k}: {v}")
        lines.append(f"          __path__: {path}")
        lines.append("")

    for s in data["services"]:
        emit_block(
            f"spring-boot-{s['name']}",
            "spring-boot",
            {"service": f"{s['name']}-platform"},
            f"{app_home}/logs/{s['name']}-platform/*.log",
        )

    for j in data.get("log_jobs", []):
        emit_block(j["name"], j["job"], j.get("labels", {}), j["path"])

    return "\n".join(lines).rstrip() + "\n"


# ---------------------------------------------------------------------------
# systemd unit
# ---------------------------------------------------------------------------

SYSTEMD_UNIT = """\
[Unit]
Description={description}
After=network.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory={app_home}
ExecStart=/usr/bin/java -Xmx768m -Xms256m -jar {app_home}/builds/sh-platform-{name}.jar --server.port={port}
Restart=on-failure
RestartSec=10
EnvironmentFile={app_home}/.env
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
"""


def render_systemd(data):
    files = {}
    for s in data["services"]:
        content = SYSTEMD_UNIT.format(
            description=s["description"],
            app_home=data["app_home"],
            name=s["name"],
            port=s["port"],
        )
        files[f"sh-platform-{s['name']}.service"] = content
    return files


# ---------------------------------------------------------------------------
# nginx 서비스 location 블록
# ---------------------------------------------------------------------------

def render_nginx_blocks(data):
    blocks = []
    for s in data["services"]:
        if s.get("routing") != "prefix":
            continue
        p = s["prefix"].strip("/")
        title = s["name"].capitalize()
        extra = ""
        if s.get("sse"):
            extra = (
                "\n"
                "        # SSE 지원을 위한 설정\n"
                "        proxy_buffering off;\n"
                "        proxy_cache off;\n"
                "        proxy_read_timeout 600s;\n"
                "        proxy_send_timeout 600s;"
            )
        blocks.append(
            f"    # === {title} API (port {s['port']}) ===\n"
            f"    location = /{p}/swagger-ui.html {{\n"
            f"        return 302 /{p}/swagger-ui/index.html;\n"
            f"    }}\n"
            f"    location = /{p}/swagger-ui {{\n"
            f"        return 302 /{p}/swagger-ui/index.html;\n"
            f"    }}\n"
            f"    location = /{p}/api-docs-ui {{\n"
            f"        return 302 /{p}/swagger-ui/index.html;\n"
            f"    }}\n"
            f"    location /{p}/ {{\n"
            f"        proxy_pass http://127.0.0.1:{s['port']}/;\n"
            f"        proxy_set_header Host $host;\n"
            f"        proxy_set_header X-Real-IP $remote_addr;\n"
            f"        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;\n"
            f"        proxy_set_header X-Forwarded-Proto $scheme;{extra}\n"
            f"    }}\n"
        )
    return "\n".join(blocks)


def inject_nginx(conf_text, block_text):
    begin_idx = conf_text.find(NGINX_BEGIN)
    end_idx = conf_text.find(NGINX_END)
    if begin_idx == -1 or end_idx == -1:
        sys.exit(f"ERROR: {NGINX_CONF} 에 마커가 없습니다 ({NGINX_BEGIN[:20]}...)")
    return conf_text[:begin_idx] + NGINX_BEGIN + "\n" + block_text + conf_text[end_idx:]


# ---------------------------------------------------------------------------
# main
# ---------------------------------------------------------------------------

def render_all():
    data = load_services()

    outputs = {
        GENERATED_DIR / "prometheus.yml": render_prometheus(data),
        GENERATED_DIR / "promtail-config.yaml": render_promtail(data),
    }
    for fname, content in render_systemd(data).items():
        outputs[GENERATED_DIR / "systemd" / fname] = content

    outputs[NGINX_CONF] = inject_nginx(
        NGINX_CONF.read_text(encoding="utf-8"), render_nginx_blocks(data)
    )
    return outputs


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true",
                        help="기존 생성물과 비교만 하고 쓰지 않음 (드리프트 검증)")
    args = parser.parse_args()

    outputs = render_all()

    if args.check:
        drift = [str(p.relative_to(ROOT)) for p, c in outputs.items()
                 if not p.exists() or p.read_text(encoding="utf-8") != c]
        if drift:
            sys.exit("DRIFT 감지 (render 필요): " + ", ".join(drift))
        print("OK: 생성물이 services.yml 과 일치합니다")
        return

    for path, content in outputs.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8")
        print(f"generated: {path.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
