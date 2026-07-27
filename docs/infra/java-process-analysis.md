---
title: Spring Boot 프로세스 관리 분석
description: gradlew bootRun vs fat JAR 배포 방식 비교 및 개선 방안
category: infra
created: 2026-07-27
updated: 2026-07-27
---

# Spring Boot 프로세스 관리 분석

## 1. 현재 상태 (2026-07-27 기준)

### 서버 리소스

| 항목 | 값 |
|------|-----|
| VM | OCI A1.Flex 2 OCPU / **5.8GB** RAM |
| Java 프로세스 수 | **12개** |
| Java 메모리 합계 | **3,192MB (3.1GB)** |
| 시스템 사용 메모리 | **3,818MB** / 5,903MB (65%) |
| 사용 가능 메모리 | **2,084MB** |

### 프로세스 구조

현재 4개 서비스가 각각 `gradlew bootRun`으로 실행 중:

```
sh-platform-auth (systemd)
  └─ gradlew java (Gradle Wrapper)      ← 128MB
       └─ GradleDaemon java             ← 256MB
            └─ Spring Boot Application  ← 512MB

sh-platform-scraper (systemd) 동일 구조
sh-platform-resume  (systemd) 동일 구조
sh-platform-portfolio (systemd) 동일 구조
```

| 구분 | 프로세스 수 | 역할 |
|------|------------|------|
| Gradle Wrapper | 4개 | gradlew 스크립트 실행 |
| Gradle Daemon | 4개 | 빌드 캐시, 의존성 해석 (전역 공유 가능) |
| Spring Boot App | 4개 | 실제 서비스 |
| **합계** | **12개** | - |

> **핵심 문제**: 서비스 1개당 3개의 Java 프로세스. Gradle Wrapper + Daemon은 서비스 운영에 불필요한 오버헤드.

---

## 2. 업계 표준 배포 방식 비교

### 방식 A: `gradlew bootRun` (현재 방식)

```bash
# systemd ExecStart
ExecStart=/home/ubuntu/sh-platform/gradlew :modules:auth:backend:bootRun --args=--server.port=8080
```

| 항목 | 내용 |
|------|------|
| 장점 | 코드 변경 즉시 반영 (빌드 자동), 개발 편의성 |
| 단점 | Java 프로세스 3배 (Wrapper+Daemon+App), 메모리 오버헤드, 시작 느림 |
| 메모리 | ~896MB / 서비스 (Wrapper 128 + Daemon 256 + App 512) |
| 프로세스 | 12개 |
| 시작 시간 | 30~60초 (GradleDaemon 초기화 포함) |
| 업데이트 | git pull → 자동 빌드 |

**적합한 경우**: 개발 서버, CI/CD에서 직접 실행
**부적합한 경우**: 프로덕션, 리소스 제한 VM

### 방식 B: Fat JAR 배포 (권장)

```bash
# 1. 빌드
./gradlew :modules:auth:backend:bootJar

# 2. systemd ExecStart
ExecStart=/usr/bin/java -Xmx384m -jar /home/ubuntu/sh-platform/builds/auth.jar --server.port=8080
```

| 항목 | 내용 |
|------|------|
| 장점 | Java 프로세스 1개/서비스, 메모리 절약, 빠른 시작, 안정적 |
| 단점 | 배포 시 빌드+파일 복사 필요, CI/CD 파이프라인 필요 |
| 메모리 | ~384MB / 서비스 (App만) |
| 프로세스 | **4개** (현재 12개 → 67% 감소) |
| 시작 시간 | 5~10초 |
| 업데이트 | git pull → 빌드 → JAR 교체 → 재시작 |

**적합한 경우**: 프로덕션 서버, 리소스 제한 환경
**대표 사례**: Netflix, LinkedIn 등 대규모 Spring Boot 운영사의 표준 방식

### 방식 C: Docker 컨테이너 배포

```dockerfile
FROM eclipse-temurin:21-jre
COPY build/libs/app.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```yaml
# docker-compose.yml
services:
  auth:
    image: sh-platform-auth
    ports: ["8080:8080"]
```

| 항목 | 내용 |
|------|------|
| 장점 | 환경 일관성,_scaling 용이, 롤백 쉬움 |
| 단점 | ARM64 호환성 이슈, 추가 학습 비용, 서버 리소스 소모 |
| 메모리 | ~384MB / 컨테이너 + Docker 오버헤드 |
| 프로세스 | 4개 + Docker 런타임 |

**적합한 경우**: 멀티 노치 배포, Kubernetes, 대규모 운영
**현재 상황**: OCI Always Free는 리소스 제한 → Docker 오버헤드 부담

### 방식 D: 빌드 서버 + SCP 배포

```bash
# 개발 머신에서 빌드
./gradlew bootJar
scp build/libs/app.jar ubuntu@140.245.95.162:/opt/sh-platform/builds/

# 서버에서
ExecStart=/usr/bin/java -jar /opt/sh-platform/builds/app.jar
```

| 항목 | 내용 |
|------|------|
| 장점 | 서버 리소스 최소화, 빌드와 배포 분리 |
| 단점 | 수동 배포, 빌드 서버 필요 |
| 메모리 | ~384MB / 서비스 |

---

## 3. 메모리 비교 시뮬레이션

### 현재 (bootRun): 12개 프로세스

| 구분 | 프로세스당 | 합계 (4서비스) |
|------|-----------|---------------|
| Gradle Wrapper | 128MB | 512MB |
| Gradle Daemon | 256MB | 1,024MB |
| Spring Boot App | 384~512MB | 1,536~2,048MB |
| **합계** | **768~896MB** | **3,072~3,584MB** |

### 개선 (Fat JAR): 4개 프로세스

| 구분 | 프로세스당 | 합계 (4서비스) |
|------|-----------|---------------|
| Spring Boot App | 384MB | 1,536MB |
| **합계** | **384MB** | **1,536MB** |

### 절감 효과

| 항목 | bootRun | Fat JAR | 절감 |
|------|---------|---------|------|
| 프로세스 수 | 12개 | 4개 | **8개 감소 (67%)** |
| 메모리 | 3.1GB | 1.5GB | **1.6GB 절감 (52%)** |
| 사용 가능 메모리 | 2.1GB | 3.7GB | **1.6GB 추가 확보** |

> 1.6GB 추가 확보 시 Prometheus, Grafana, DB 등 다른 서비스를 위한 여유 확보 가능.

---

## 4. 시작 시간 비교

| 항목 | bootRun | Fat JAR |
|------|---------|---------|
| GradleDaemon 시작 | 10~20초 | - |
| 의존성 해석 | 5~10초 | - |
| Spring Boot 시작 | 20~30초 | 5~10초 |
| **총 시작 시간** | **35~60초** | **5~10초** |

Fat JAR는 Gradle 없이 바로 Spring Boot가 시작되므로 **6배 빠름**.

---

## 5. 안정성 비교

| 항목 | bootRun | Fat JAR |
|------|---------|---------|
| GradleDaemon 크래시 | 서비스 전체 중단 가능 | 해당 서비스만 영향 |
| 메모리 부족 시 | Daemon + App 동시 중단 | App만 스왑 |
| 업데이트 중 중단 | gradlew 프로세스 종료 시 전체 영향 | 개별 서비스 독립 |
| 프로세스 관리 | systemd가 3개 프로세스 추적 | systemd가 1개 프로세스 추적 |

---

## 6. 업계 사례

| 회사/플랫폼 | 배포 방식 | 비고 |
|-------------|----------|------|
| Netflix | Fat JAR + AMI | 수백 개 Spring Boot 서비스 |
| LinkedIn | Fat JAR + 자동 배포 | 대규모 마이크로서비스 |
| Spring 공식 권장 | Fat JAR (`bootJar`) | 프로덕션 표준 권장 |
| AWS Elastic Beanstalk | Fat JAR | 표준 배포 방식 |
| Heroku | Fat JAR | 프로덕션 배포 |
| Dev/테스트 환경 | `bootRun` | 개발 편의성 |

> **결론**: 업계 표준은 **Fat JAR**. `bootRun`은 개발 환경 전용.

---

## 7. 개선 방안 권장

### 권장: Fat JAR 전환 (방식 B)

**이유**:
1. OCI Always Free는 리소스 제한 (2 OCPU / 5.8GB) → 메모리 절약 필수
2. 프로세스 12개 → 4개로 줄어들면 관리 용이
3. 시작 시간 6배 빠름 → 재시작/배포 시 다운타임 감소
4. 업계 표준 → 유지보수 용이

**변경 내용**:

```bash
# 현재: bootRun
ExecStart=/home/ubuntu/sh-platform/gradlew :modules:auth:backend:bootRun --args=--server.port=8080

# 변경: Fat JAR
ExecStart=/usr/bin/java -Xmx384m -Xms128m \
  -jar /home/ubuntu/sh-platform/builds/auth-backend.jar \
  --server.port=8080
```

**CI/CD 변경**:
```yaml
# deploy-backend.yml에서
- name: Build fat JAR
  run: ./gradlew :modules:${{ matrix.module }}:backend:bootJar

- name: Deploy JAR
  run: scp build/libs/*.jar ubuntu@server:/home/ubuntu/sh-platform/builds/
```

**systemd 변경**:
```ini
[Service]
ExecStart=/usr/bin/java -Xmx384m -Xms128m \
  -jar /home/ubuntu/sh-platform/builds/${SERVICE_NAME}.jar \
  --server.port=${PORT}
Restart=always
RestartSec=10
```

### 예상 결과

| 항목 | 현재 | 개선 후 |
|------|------|---------|
| Java 프로세스 | 12개 | 4개 |
| 메모리 사용 | 3.1GB | 1.5GB |
| 사용 가능 메모리 | 2.1GB | 3.7GB |
| 시작 시간 | 35~60초 | 5~10초 |
| 시스템 부하 | 높음 | 낮음 |

---

## 8. 참고: 실제 프로세스 출력

```
# 현재 `ps -ef | grep java` 출력 (요약)
ubuntu  1933521  1  gradlew wrapper (resume)
ubuntu  1933522  1  gradlew wrapper (portfolio)
ubuntu  1933574  1933521  GradleDaemon (resume)
ubuntu  1933589  1933522  GradleDaemon (portfolio)
ubuntu  1933885  1933589  ResumePlatformApplication
ubuntu  1933913  1933574  PortfolioPlatformApplication
ubuntu  2240438  1  GradleDaemon (auth/scraper 공유)
ubuntu  2274157  1  gradlew wrapper (auth)
ubuntu  2274227  2240438  AuthPlatformApplication
ubuntu  2274328  1  gradlew wrapper (scraper)
ubuntu  2274361  2274328  GradleDaemon (scraper)
ubuntu  2274652  2274361  ScraperPlatformApplication
```
