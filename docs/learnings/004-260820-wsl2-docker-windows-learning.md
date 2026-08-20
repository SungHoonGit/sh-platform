# 004-260820-wsl2-docker-windows-learning

## 개요
- **주제**: Windows에서 WSL2 + Docker Desktop으로 로컬 개발 DB(MariaDB)를 구성하는 방법
- **학습일**: 2026-08-20
- **수준**: 중급

## 1. 개념 설명

### 1.1 정의
- **WSL2** (Windows Subsystem for Linux 2): Windows 위에서 경량 Linux 커널을 가상머신으로 실행하는 계층. Docker 컨테이너(Linux 프로세스)를 Windows에서 직접 실행 가능하게 해줌.
- **Docker**: 컨테이너 기반 애플리케이션 실행 플랫폼. 이미지(파일 시스템 스냅샷)로 어디서든 동일한 환경을 보장.
- **Docker Desktop**: Windows/macOS용 Docker GUI+CLI. 내부적으로 WSL2 백엔드 사용.

### 1.2 왜 필요한가
- Docker 컨테이너는 **본질적으로 Linux 프로세스** → Windows 위에 직접 실행 불가
- 경량 Linux VM(WSL2)을 하나 만들고 그 안에서 컨테이너를 실행하는 구조
- 팀원 OS가 달라도 같은 `docker-compose.yml` 하나로 동일한 로컬 환경 구성

### 1.3 관련 개념
- **이미지(Image)**: 컨테이너의 템플릿. git에 안 올리고 Docker Hub에서 다운로드
- **컨테이너(Container)**: 이미지를 실행한 인스턴스
- **docker-compose.yml**: 여러 컨테이너/설정을 파일로 선언 (버전 관리됨 = git 공유 대상)
- **Kubernetes**: 컨테이너 **오케스트레이션** (여러 서버의 컨테이너 관리) — Docker와 계층이 다름

## 2. 설정 방법

### 2.1 사전 조건
- Windows 10/11
- 관리자 권한 PowerShell

### 2.2 설치/설정 단계

**1단계: Docker Desktop 설치**
```powershell
winget install -e --id Docker.DockerDesktop
```

**2단계: WSL2 설치** (관리자 PowerShell)
```powershell
wsl --install
```
> 재부팅 필요할 수 있음

**3단계: 가상화 기능 활성화** (관리자 PowerShell)
```powershell
dism.exe /online /enable-feature /featurename:VirtualMachinePlatform /all /norestart
dism.exe /online /enable-feature /featurename:Microsoft-Windows-Subsystem-Linux /all /norestart
```
> **오류 740**: 관리자 권한이 아닌 PowerShell에서 실행했을 때 발생 → "관리자 권한으로 실행"

**4단계: 상태 확인**
```powershell
wsl --status          # "기본 버전: 2" 확인
docker version        # Server 섹션이 떠야 정상
```

### 2.3 문제 해결: "가상화가 활성화되지 않은 컴퓨터"

WSL2가 "virtualization not enabled"라고 하면:
1. `dism`으로 VirtualMachinePlatform 활성화 (위 3단계)
2. 재부팅
3. 그래도 안 되면 **BIOS에서 VT-x 켜기**:
   - Lenovo ThinkPad: 전원 후 **F1** 연타 (또는 Fn+F1, Novo 버튼)
   - `Config → CPU` 또는 `Advanced` 탭 → `Intel Virtualization Technology` → `Enabled`
   - F10 저장 후 재부팅

### 2.4 Docker Desktop 재시작 방법
```powershell
Stop-Process -Name "Docker Desktop" -ErrorAction SilentlyContinue
Start-Process "C:\Program Files\Docker\Docker\Docker Desktop.exe"
```

## 3. 사용법

### 3.1 기본 사용
```bash
docker compose up -d        # 컨테이너 백그라운드 기동
docker compose down         # 컨테이너 종료
docker compose down -v      # 데이터까지 삭제 (초기화)
docker ps                   # 실행 중 컨테이너 확인
docker logs <컨테이너명>    # 로그 확인
```

### 3.2 이 프로젝트에서의 적용
- 루트 `docker-compose.yml`로 로컬 MariaDB 10.11 컨테이너 기동 (설계 문서: `docs/plans/010-260820-local-dev-docker-design.md`)
- 4개 DB 스키마(sh_pass, scraper_platform, resume_platform, portfolio_platform)를 하나의 컨테이너로 서비스
- 각 모듈은 `application-local.yml`(localhost:3306)로 접속

## 4. 주의사항
- **Docker 명령 PATH**: 설치 직후 현재 셸에 PATH 미반영 → `C:\Program Files\Docker\Docker\resources\bin` 추가하거나 셸 재시작
- **com.docker.service가 Stopped**: Docker Desktop 실행 시 자동 시작됨 (수동 시작 불가 시 Docker Desktop 재실행)
- **재부팅 필요**: WSL2/VirtualMachinePlatform 기능 활성화 후 재부팅 필수일 수 있음
- **Windows PowerShell 오류 740**: 관리자 권한 필수

## 5. Docker vs Kubernetes

### 5.1 핵심 차이 (비유)
- **Docker** = 개별 **택배 박스**(컨테이너)에 물건을 포장하는 시스템
- **Kubernetes (K8s)** = 그 박스들을 **물류센터에서 관리하는 시스템** (박스 몇 개, 어디에, 언제 배송할지 자동 조정)

### 5.2 기술 비교

| | Docker | Kubernetes (K8s) |
|--|--------|------------------|
| 역할 | 컨테이너 **실행** | 컨테이너 **오케스트레이션** |
| 단위 | 컨테이너 1개 관리 | 수백 개 서버의 컨테이너 관리 |
| 서버 수 | 1대 | 3대 이상 (클러스터) |
| 자동 확장 | ❌ (수동) | ✅ 자동 (HPA) |
| 장애 복구 | ❌ (죽으면 끝) | ✅ 자동 재시작/재배치 |
| 부하 분산 | ❌ | ✅ (Service/Ingress) |
| 설정 갱신 | 수동 | ✅ 롤링 업데이트/롤백 |

### 5.3 세트로 쓰는 이유
- Docker가 컨테이너를 만들고, **K8s가 그걸 여러 서버에 걸쳐 관리** → 세트로 언급됨
- K8s도 컨테이너 런타임으로 Docker(의 API 호환 버전)를 사용

### 5.4 K8s 도입 시점 판단 기준

K8s가 필요한 때 (아래 **모두**에 가까울수록):
- 서버 3대 이상 (장애/부하 분산 목적의 클러스터)
- 마이크로서비스 10개 이상
- 자동 스케일링, 무중단 배포가 필요
- 팀 규모가 커서 배포 파이프라인 자동화가 필수

K8s가 **불필요**한 때 (현재 SH Platform):
- 서버 1~2대
- 서비스 4~5개
- systemd + GitHub Actions로 충분히 배포 자동화 중

### 5.5 SH Platform 미래 시나리오

| 단계 | 규모 | 배포 방식 |
|------|------|-----------|
| 현재 | OCI 1대, 서비스 4개 | **systemd** (유지) |
| 로컬 개발 | PC 여러 대 | **Docker Compose** (DB만) |
| 성장 1단계 | 서버 2대 + 이중화 | 서버별 systemd + nginx 라우팅 분리 |
| 성장 2단계 | 서버 3대 이상, 서비스 10개+ | **K8s 전환 검토** |

> **이중화 라우팅만으로는 K8s가 필요 없습니다.** 서버 2대 이중화 + nginx 리버스 프록시 + DB 복제(HA)면 충분. K8s는 그보다 규모가 커진 **성장 2단계**에서 검토할 것.

## 6. 참고 자료
- [Docker Desktop 공식 문서](https://docs.docker.com/desktop/)
- [WSL 설치 가이드](https://learn.microsoft.com/ko-kr/windows/wsl/install)
- [WSL 가상화 문제](https://aka.ms/enablevirtualization)

---
*작성일: 2026-08-20*