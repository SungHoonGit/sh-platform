# 008-260821 인프라 설정 단일 소스(SSOT) 가이드

## 개요
- **목적**: 서비스 추가/변경 시 여러 인프라 설정파일을 일일이 수동 수정하는 문제를 해소한다. 서비스 정보를 한 곳(infra/services.yml)에만 정의하고 나머지 설정은 자동 생성한다.
- **대상**: sh-platform 유지보수자, AI 에이전트
- **작성일**: 2026-08-21

## 1. 개념

### 1.1 정의

**SSOT (Single Source of Truth)**: 하나의 데이터(서비스명·포트·경로)의 원본을 한 곳에만 두고, 나머지는 그것으로부터 파생시키는 구조.

**GitOps**: 인프라 설정의 원본을 git에 두고, 배포 파이프라인이 서버에 적용하는 운영 방식. "서버에 있는 파일은 git의 복사본일 뿐"이 원칙.

### 1.2 왜 필요한가

모듈 하나를 추가하면 원래 아래 **5곳**에 같은 "서비스명+포트"를 반복 입력해야 했다:

| 파일 | 반복 내용 |
|------|-----------|
| `/etc/prometheus/prometheus.yml` | 스크랩 타겟 `localhost:{port}` |
| `/etc/promtail/promtail-config.yaml` | 로그 job `{name}-platform` |
| `/etc/systemd/system/sh-platform-{name}.service` | 유닛 전체 |
| nginx conf | location 블록 `{prefix}` → `{port}` |
| deploy 워크플로우 | 빌드/JAR복사/재시작 줄 |

실제로 portfolio 제거 때 prometheus 타겟 수동 삭제가 발생했다. 모듈이 늘어날수록 누락 위험이 커진다.

### 1.3 확장 단계 (업계 표준 로드맵)

| 단계 | 방식 | 도입 시점 |
|------|------|-----------|
| 1. git 관리 + CI 복사 | 설정파일을 레포에서 관리, 워크플로우가 cp | 단일 VM ← **현재 위치** |
| 2. SSOT + 렌더링 | services.yml 하나만 편집, 나머지 자동 생성 | ← **이번에 도입** |
| 3. Ansible (IaC) | VM 여러 대, 멱등 프로비저닝 | 서버 증설 시 |
| 4. k8s + Argo CD/Flux | 컨테이너 오케스트레이션 GitOps | 컨테이너 전환 시 |
| (대안) Consul/Eureka | 런타임 서비스 디스커버리 — 앱이 자기 위치 등록 | 동적 오토스케일링 시 |

## 2. 구조

```
infra/
├── services.yml              # ★ 유일하게 손으로 편집하는 파일
├── generated/                # 생성물 (git 추적 — diff 리뷰 가능)
│   ├── prometheus.yml
│   ├── promtail-config.yaml
│   └── systemd/*.service
└── nginx/sh-platform.conf    # 마커 영역(# === [AUTO-GENERATED...)에 서비스 블록 주입

scripts/render_config.py       # 생성기 (PyYAML 사용, 로컬 실행)
```

생성 흐름:

```
infra/services.yml ──(python scripts/render_config.py)──▶ infra/generated/*
                                                              │
                                              deploy-backend.yml이 서버에 cp
                                                              ▼
                              /etc/prometheus, /etc/promtail, /etc/systemd/system
```

## 3. 사용법

### 3.1 설정 변경 후 렌더링 (필수)

```bash
python scripts/render_config.py           # 생성 + nginx 주입
python scripts/render_config.py --check   # 드리프트 검증만 (생성물이 최신인지)
```

- `services.yml`을 고쳤는데 렌더를 잊으면 `--check`가 실패한다 (CI 추가 가능).
- 생성물(`infra/generated/`)도 커밋하므로 PR에서 무엇이 바뀌는지 보인다.

### 3.2 새 모듈 추가 체크리스트

1. `infra/services.yml`에 서비스 추가:
   ```yaml
     - name: resume            # gradle/systemd/로그 디렉토리 공통 접미사
       port: 8082
       description: SH Platform Resume Service
       routing: prefix         # prefix | custom
       prefix: /resume         # routing=prefix인 경우 필수
       sse: true               # 선택 (SSE 프록시 옵션)
   ```
2. `python scripts/render_config.py`
3. 남은 수동 작업 (구조상 자동화 안 함):
   - Gradle 모듈 생성 (`settings.gradle.kts`)
   - deploy 워크플로우 빌드/JAR복사/재시작 줄
   - nginx의 특수 블록 (frontend alias 등, 마커 밖)
4. 커밋 → push → 자동 배포로 서버 반영까지 끝.

### 3.3 포트 변경

`services.yml`의 port만 수정 → render → push. systemd ExecStart, prometheus 타겟, nginx 프록시가 한 번에 바뀐다.

## 4. 문제 해결

| 문제 | 원인 | 해결책 |
|------|------|--------|
| `--check` 실패 (DRIFT) | services.yml 수정 후 미렌더 | `python scripts/render_config.py` 재실행 |
| nginx 주입 위치가 이상함 | 마커 줄이 지워짐 | `sh-platform.conf`에서 `[AUTO-GENERATED SERVICE BLOCKS]` 시작/끝 마커 복원 |
| Grafana 알림은 왜 자동? | 규칙이 `up{job="spring-boot"}` instance 라벨 기반 | 서비스 추가/삭제와 무관하게 동작 (docs/infra/grafana-alert-rules.yml 참고) |
| 서버에서 직접 고쳐버림 | — | 다음 배포 때 레포 버전으로 덮여씀 (nginx는 기존 규칙과 동일) |

## 5. 참고 자료
- 설계 경위: `docs/plans/011-260821-resume-portfolio-integration-design.md` Phase 1 후속
- Grafana 알림 규칙 백업: `docs/infra/grafana-alert-rules.yml`
- nginx 배포 규칙: `infra/nginx/sh-platform.conf` 헤더 주석
- GitOps 개요: https://opengitops.dev/

---
*작성일: 2026-08-21*
