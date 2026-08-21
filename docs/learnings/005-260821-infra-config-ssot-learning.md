# 005-260821 인프라 설정 중앙화 패턴(SSOT·IaC·GitOps) 학습 기록

## 개요
- **주제**: 서비스 정보의 단일 소스(SSOT) + 템플릿 렌더링으로 인프라 설정 자동 생성하는 패턴 (Ansible/Helm과 같은 형태)
- **학습일**: 2026-08-21
- **수준**: 중급

## 1. 개념 설명

### 1.1 정의

| 용어 | 정의 |
|------|------|
| **SSOT** (Single Source of Truth) | 하나의 데이터(서비스명·포트 등)의 원본을 오직 한 곳에만 두고, 나머지는 파생물로 관리하는 원칙 |
| **IaC** (Infrastructure as Code) | 서버·설정·네트워크 등 인프라를 수동 작업이 아닌 코드로 기술하는 것 (Terraform, Ansible) |
| **템플릿 렌더링** | 원본 값(inventory/values)을 템플릿에 채워 최종 설정파일을 생성하는 기법 |
| **GitOps** | git 저장소가 선언된 상태의 원본이고, 파이프라인/에이전트가 실제 환경을 그 상태로 동기화하는 운영 방식 |
| **Drift** | 선언된 상태(git)와 실제 상태(서버)의 불일치 |

이번에 구현한 것 = **"inventory + template rendering" 패턴의 소형판**.
- Ansible: `inventory`(호스트변수 yml) + `templates/*.j2`(Jinja2) → 서버에 적용
- Helm(k8s): `values.yaml` + `templates/` → 매니페스트 생성
- sh-platform: `infra/services.yml` + `scripts/render_config.py` → prometheus/promtail/systemd/nginx 생성

세 가지 모두 **"값의 원본 1개 + 생성 규칙"**이라는 동일한 형태다.

### 1.2 왜 필요한가

모듈 하나 추가 시 같은 "서비스명+포트"를 여러 파일에 반복 입력해야 한다:

```
prometheus.yml (타겟) · promtail (로그 job) · systemd 유닛 · nginx location · 배포 워크플로우
```

반복 입력은 반드시 누락을 낳는다. 실제 사례(2026-08-21): portfolio 모듈 제거 후
prometheus 스크랩 타겟에서 localhost:8083 삭제를 잊어 "서비스 다운" 알림이 발화할 뻔했다.
DRY 원칙을 인프라에 적용한 것이 이 패턴이다.

### 1.3 관련 개념

- **Configuration Management**: Ansible/Puppet/Chef — 서버 설정을 코드로 관리하는 도구 계보
- **Provisioning**: Terraform/CloudFormation — 서버 자체(VM·네트워크) 생성을 코드로
- **패키지 템플릿**: Helm/Kustomize — k8s 매니페스트의 값 분리·오버레이
- **Service Discovery**: Consul/Eureka/k8s DNS — 설정이 아니라 **런타임에** 서비스 위치를 해결하는 대안. 앱이 시작 시 자기 위치를 등록하면 프롬메테우스·nginx가 레지스트리를 조회. 동적 오토스케일링에 적합하지만 소규모 정적 환경에선 과함
- **Twelve-Factor App (3번 설정)**: 설정을 환경과 코드에서 분리하라는 원칙 — SSOT은 그 확장

## 2. 사용법

### 2.1 기본 사용 (sh-platform 기준)

```bash
# 1. 원본만 수정
vim infra/services.yml        # 서비스 추가/포트 변경

# 2. 렌더링 (생성물 갱신 + nginx 마커 영역 주입)
python scripts/render_config.py

# 3. 커밋 & 푸시 → 워크플로우가 서버에 cp + reload
git add infra && git commit && git push
```

### 2.2 고급 사용

```bash
# 드리프트 검증: services.yml 수정 후 렌더를 잊었으면 실패한다
python scripts/render_config.py --check
```

설계 선택지 비교:

| 방식 | 장단점 |
|------|--------|
| 생성물도 커밋 (채택) | PR diff로 변경 리뷰 가능, 서버에 python 불필요. 단, 렌더 잊으면 drift → `--check`로 보완 |
| CI에서만 렌더링 | 로컬 도구 불필요. 단, PR에서 최종 설정이 안 보임 |

## 3. 주의사항

- **생성물 수동 수정 금지**: 다음 렌더링에 덮여씀. nginx는 마커(`[AUTO-GENERATED ...]`) 안쪽만 자동 영역
- **시크릿은 SSOT에 넣지 않는다**: 비밀번호/키는 `.env`나 Vault 등 별도 경로 (services.yml엔 포트·경로만)
- **routing: custom 예외 존재**: auth처럼 root 경로가 여러 개인 서비스는 nginx 블록을 수동 관리 — 자동화는 균일한 패턴에만 적용
- **SSOT 스키마가 커지면 문서화 필수**: 어떤 필드가 어느 파일로 파생되는지 가이드 유지

## 4. 실전 적용

### 4.1 이 프로젝트에서의 적용

- 2026-08-21 portfolio 제거 작업 중 prometheus 타겟 수동 삭제 이슈 → 근본 해결로 SSOT 도입
- 서비스 추가 절차가 "5곳 수정" → "yml 4줄 추가 + render 1회"로 축소

### 4.2 관련 코드

- `infra/services.yml`: 단일 소스 (services / static_targets / log_jobs)
- `scripts/render_config.py`: 렌더러 (`--check` 드리프트 검증 내장)
- `infra/generated/*`: 생성물 (git 추적)
- `infra/nginx/sh-platform.conf`: 마커 영역 자동 주입
- `.github/workflows/deploy-backend.yml`: 서버 동기화 스텝
- 가이드: `docs/guides/008-260821-infra-ssot-guide.md`

## 5. 참고 자료
- [OpenGitOps](https://opengitops.dev/) — GitOps 원칙 4가지
- [Ansible Inventory](https://docs.ansible.com/ansible/latest/inventory_guide/intro_inventory.html) — inventory + 변수 개념
- [Helm Charts](https://helm.sh/docs/topics/charts/) — values.yaml + templates 구조
- [Twelve-Factor App: Config](https://12factor.net/ko/config)

---
*작성일: 2026-08-21*
