---
title: Logging Guide
description: Logging Guide - guide module documentation
category: guide
created: 2026-07-14
updated: 2026-07-21
---

# 중앙 집중 로깅 사용 가이드

## 1. Grafana에서 로그 조회

### 1.1 Explore에서 로그 검색

1. Grafana 접속: https://sunghoonyk.duckdns.org/grafana/login
2. 좌측 메뉴 -> Explore
3. 상단에서 Loki 선택
4. 쿼리 입력 -> Run query 클릭

### 1.2 기본 쿼리 예시

\`\`\`logql
# 모든 spring-boot 로그
{job="spring-boot"}

# 모든 nginx 로그
{job="nginx"}

# nginx access 로그만
{job="nginx", type="access"}

# nginx error 로그만
{job="nginx", type="error"}

# syslog
{job="syslog"}
\`\`\`

### 1.3 필터링

\`\`\`logql
# ERROR 로그만
{job="spring-boot"} |= "ERROR"

# DEBUG 제외
{job="spring-boot"} != "DEBUG"

# timeout 또는 error 포함
{job="spring-boot"} |~ "timeout|error"

# 대소문자 구분 없이 검색
{job="spring-boot"} |~ "(?i)error"
\`\`\`

---

## 2. LogQL 연산자 상세

### 2.1 문자열 필터

| 연산자 | 의미 | 예시 |
|--------|------|------|
| |= | 문자열 포함 | |= "ERROR" |
| \|~ | 정규식 매칭 | \|~ "timeout\|error" |
| != | 문자열 불포함 | != "DEBUG" |
| !~ | 정규식 불매칭 | !~ "health" |

### 2.2 파이프라인

\`\`\`logql
# 여러 필터 체이닝
{job="spring-boot"} |= "ERROR" != "DEBUG" |~ "timeout"

# 로그 파싱 (logfmt)
{job="spring-boot"} | logfmt | level="error"

# JSON 파싱
{job="spring-boot"} | json | status >= 500

# 특정 필드 추출
{job="spring-boot"} | logfmt | line_format "{{.message}}"
\`\`\`

### 2.3 메트릭 변환

\`\`\`logql
# 에러 로그 비율 (초당)
rate({job="spring-boot"} |= "ERROR" [5m])

# 에러 카운트 (5분 합계)
sum(rate({job="spring-boot"} |= "ERROR" [5m]))

# job별 에러 비율
sum(rate({job="spring-boot"} |= "ERROR" [5m])) by (job)
\`\`\`

---

## 3. 실용적인 쿼리 모음

### 3.1 Spring Boot

\`\`\`logql
# 모든 에러 로그
{job="spring-boot"} |= "ERROR"

# WARN 이상
{job="spring-boot"} |~ "WARN|ERROR|FATAL"

# 특정 컨트롤러 에러
{job="spring-boot"} |= "ERROR" |= "Controller"

# DB 관련 에러
{job="spring-boot"} |~ "SQLException|DataAccessException|timeout"

# JWT/인증 에러
{job="spring-boot"} |~ "InvalidToken|TokenExpired|Authentication"

# 최근 5분간 에러 수
sum(rate({job="spring-boot"} |= "ERROR" [5m]))
\`\`\`

### 3.2 nginx

\`\`\`logql
# 5xx 에러
{job="nginx"} |= "500" or |= "502" or |= "503" or |= "504"

# 4xx 에러
{job="nginx"} |~ " [45]\\\\d{2} "

# 특정 경로 접근
{job="nginx"} |= "/api/v1/auth"

# 특정 IP 접근
{job="nginx"} |= "192.168.1.100"

# 접근 로그 파싱
{job="nginx", type="access"} | logfmt | status >= 400
\`\`\`

### 3.3 시스템

\`\`\`logql
# 인증 실패
{job="auth"} |= "Failed password"

# sudo 사용
{job="auth"} |= "sudo"

# systemd 서비스 상태 변경
{job="syslog"} |~ "Started|Stopped|Failed"
\`\`\`

---

## 4. Grafana 대시보드에 로그 패널 추가

### 4.1 로그 패널 생성

1. 대시보드 -> Add panel -> Logs 선택
2. Data source: Loki
3. 쿼리 입력

### 4.2 추천 패널 구성

| 패널 이름 | 쿼리 | 설명 |
|-----------|------|------|
| Spring Boot Errors | {job="spring-boot"} |= "ERROR" | 앱 에러 로그 |
| nginx 5xx Errors | {job="nginx"} |= "500" or |= "502" | 서버 에러 |
| Auth Failures | {job="auth"} |= "Failed" | 인증 실패 |
| System Logs | {job="syslog"} | 시스템 로그 |

### 4.3 로그 패널 설정

| 설정 | 권장값 |
|------|--------|
| Lines limit | 1000 |
| Dedup strategy | none |
| Order | Newest first |

---

## 5. 메트릭과 로그 연동

### 5.1 Grafana에서 로그에서 메트릭 전환

1. Explore -> Loki 선택
2. 로그 쿼리 실행
3. 상단 Metric 버튼 클릭
4. LogQL에서 메트릭 추출

### 5.2 예시: 에러율 메트릭화

\`\`\`logql
# 로그 쿼리
{job="spring-boot"} |= "ERROR"

# Metric으로 변환
sum(rate({job="spring-boot"} |= "ERROR" [5m])) by (job)
\`\`\`

### 5.3 대시보드에서 메트릭에서 로그 전환

1. 메트릭 패널 (Prometheus) -> 패널 메뉴 -> View logs
2. 자동으로 Loki 쿼리 생성
3. 해당 시간대의 로그 표시

---

## 6. 알림 설정

### 6.1 로그 기반 알림

1. Grafana -> Alerting -> Alert rules
2. New alert rule 클릭
3. Data source: Loki
4. 쿼리 예시:

\`\`\`logql
# 에러 로그가 5분간 10개 이상일 때
sum(rate({job="spring-boot"} |= "ERROR" [5m])) > 0.1
\`\`\`

### 6.2 알림 조건 예시

| 조건 | LogQL | 설명 |
|------|-------|------|
| 에러율 급증 | sum(rate({job="spring-boot"} |= "ERROR" [5m])) > 0.1 | 초당 0.1건 이상 |
| 인증 실패 | sum(rate({job="auth"} |= "Failed" [5m])) > 0.05 | 초당 0.05건 이상 |
| nginx 5xx | sum(rate({job="nginx"} |= "500" [5m])) > 0.01 | 초당 0.01건 이상 |

---

## 7. 문제 해결

### Q: No data 표시

\`\`\`logql
# 1. 라벨 확인
{job="spring-boot"}

# 2. 시간 범위 확인 (우측 상단 타임피커)
# 3. 필터 조건 완화
{job="spring-boot"} |= ""
\`\`\`

### Q: 검색 속도 느림

\`\`\`logql
# 1. 라벨로 먼저 필터
{job="spring-boot"}

# 2. 시간 범위 줄이기
# 3. 필터 단순화
{job="spring-boot"} |= "ERROR"
\`\`\`

### Q: 로그가 안 나옴

\`\`\`bash
# 1. Promtail 상태 확인
sudo systemctl status promtail

# 2. Loki 상태 확인
curl -s http://localhost:3100/ready

# 3. Promtail 위치 파일 확인
cat /var/lib/promtail/positions.yaml

# 4. 로그 파일 존재 확인
ls -la /home/ubuntu/sh-platform/logs/
ls -la /var/log/nginx/
\`\`\`

---

## 8. CLI에서 LogQL 사용 (logcli)

### 8.1 logcli 설치

\`\`\`bash
cd /tmp
curl -sL -o logcli-linux-arm64.zip https://github.com/grafana/loki/releases/download/v3.7.3/logcli-linux-arm64.zip
unzip -o logcli-linux-arm64.zip
sudo mv logcli-linux-arm64 /usr/local/bin/logcli
sudo chmod +x /usr/local/bin/logcli
\`\`\`

### 8.2 사용법

\`\`\`bash
# 라벨 확인
logcli labels

# job 값 확인
logcli labels job

# spring-boot 로그 조회
logcli query {job="spring-boot"} |= "ERROR"

# 최근 1시간 nginx 5xx
logcli query --since=1h {job="nginx"} |= "500"
\`\`\`

---

## 9. 로그 보관 정책

| 로그 유형 | 보관 기간 | 설정 위치 |
|-----------|-----------|-----------|
| Spring Boot | 7일 | Loki compactor |
| nginx | 7일 | Loki compactor |
| syslog | 7일 | Loki compactor |
| auth.log | 30일 | 추후 설정 |

---

## 10. 참고 자료

- [개념 가이드](logging-concept.md)
- [설치 가이드](logging-install.md)
- [Grafana 사용 가이드](grafana-guide.md)
- [모니터링 설정 가이드](monitoring-guide.md)
