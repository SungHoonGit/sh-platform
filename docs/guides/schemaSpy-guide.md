---
title: Schemaspy Guide
description: Schemaspy Guide - guide module documentation
category: guide
created: 2026-07-13
updated: 2026-07-21
---

# SchemaSpy - DB 문서 자동화 가이드

## 1. 개요

SchemaSpy는 데이터베이스 메타데이터를 분석하여 **HTML 형태의 ERD, 테이블 관계, 컬럼 상세** 문서를 자동 생성한다.

```
DB 메타데이터 분석
  → 테이블 목록, 컬럼, 제약조건, 인덱스
  → HTML 문서 + ERD 다이어그램 (dot/graphviz)
  → 브라우저에서 열람 가능
```

### 장점

| 항목 | 효과 |
|------|------|
| 수동 작업 제거 | SQL DDL 문서화 자동화 |
| 시각적 ERD | 테이블 관계를 다이어그램으로 자동 생성 |
| 항상 최신 | DB 변경 시 재실행으로 자동 반영 |
| 팀 공유 | HTML을 nginx로 서빙하여 팀원 모두 열람 가능 |

---

## 2. 설치 및 설정

### 2.1 필수 요소

```bash
# Java 21 (이미 설치됨)
java --version

# Graphviz (ERD 생성용)
sudo apt-get install -y graphviz
```

### 2.2 SchemaSpy 다운로드

```bash
# /opt/schemaSpy 디렉토리 생성
sudo mkdir -p /opt/schemaSpy

# SchemaSpy JAR 다운로드
cd /opt/schemaSpy
sudo wget https://github.com/schemaspy/schemaspy/releases/download/v6.2.4/schemaspy-6.2.4.jar

# MariaDB JDBC 드라이버 다운로드
sudo wget https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.5.3/mariadb-java-client-3.5.3.jar

# 실행 권한 설정
sudo chmod +x /opt/schemaSpy/*.jar
```

### 2.3 출력 디렉토리 생성

```bash
sudo mkdir -p /var/www/schemaSpy
sudo chown ubuntu:ubuntu /var/www/schemaSpy
```

---

## 3. 실행 스크립트

### 3.1 스크립트 생성

```bash
cat << 'SCRIPT' > /opt/schemaSpy/run-schemaSpy.sh
#!/bin/bash

# 설정
DB_HOST="10.0.0.39"
DB_PORT="3306"
DB_NAME="sh_pass"
DB_USER="sh_user"
DB_PASS="SHpass1234!"
OUTPUT_DIR="/var/www/schemaSpy"
SCHEMASPY_JAR="/opt/schemaSpy/schemaspy-6.2.4.jar"
JDBC_JAR="/opt/schemaSpy/mariadb-java-client-3.5.3.jar"

# 실행
java -jar "$SCHEMASPY_JAR" \
  -t mariadb \
  -host "$DB_HOST" \
  -port "$DB_PORT" \
  -db "$DB_NAME" \
  -u "$DB_USER" \
  -p "$DB_PASS" \
  -dp "$JDBC_JAR" \
  -o "$OUTPUT_DIR" \
  -s sh_pass

echo "문서 생성 완료: $OUTPUT_DIR"
SCRIPT

chmod +x /opt/schemaSpy/run-schemaSpy.sh
```

### 3.2 실행

```bash
# SchemaSpy 실행
sudo /opt/schemaSpy/run-schemaSpy.sh

# 결과 확인
ls -la /var/www/schemaSpy/
```

---

## 4. nginx 설정

### 4.1 nginx 서버 블록 수정

```bash
sudo nano /etc/nginx/sites-available/sh-platform
```

```nginx
# SchemaSpy 문서 서빙
location /schemaSpy/ {
    alias /var/www/schemaSpy/;
    index index.html;
    
    # 캐시 설정 (문서는 자주 변경 안 됨)
    expires 1d;
    add_header Cache-Control "public, immutable";
}
```

### 4.2 nginx 리로드

```bash
sudo nginx -t && sudo systemctl reload nginx
```

---

## 5. 접속 경로

| 환경 | URL |
|------|-----|
| 운영 | https://sunghoonyk.duckdns.org/schemaSpy/ |
| 로컬 | 파일 탐색기로 직접 열람 |

---

## 6. 자동화 (선택)

### 6.1 GitHub Actions에 추가

`.github/workflows/deploy-backend.yml`에 추가:

```yaml
- name: Generate DB Documentation
  run: |
    ssh ${{ secrets.OCI_SSH_USER }}@${{ secrets.OCI_WEB_IP }} \
      "sudo /opt/schemaSpy/run-schemaSpy.sh"
```

### 6.2 Cron Job (주 1회 자동 실행)

```bash
# crontab 편집
crontab -e

# 매주 일요일 오전 3시에 실행
0 3 * * 0 /opt/schemaSpy/run-schemaSpy.sh >> /var/log/schemaSpy.log 2>&1
```

---

## 7. 출력 결과

### 생성되는 파일

```
/var/www/schemaSpy/
├── index.html          # 메인 페이지
├── tables/             # 테이블별 상세 페이지
│   ├── users.html
│   ├── refresh_tokens.html
│   └── ...
├── diagrams/           # ERD 다이어그램
│   ├── relationships.png
│   └── ...
└── anomalies/          # 이상 탐지 (FK 없음 등)
```

### 확인 내용

- 테이블 목록 및 관계
- 컬럼 상세 (타입, Null 여부, 기본값)
- 인덱스 목록
- FK 관계 다이어그램
- 이상 탐지 (FK 없는 테이블 등)

---

## 8. 문제 해결

### Q: Graphviz 에러 발생

```bash
sudo apt-get install -y graphviz
```

### Q: JDBC 드라이버 연결 실패

```bash
# MariaDB가 외부 연결 허용하는지 확인
# MariaDB 서버에서:
sudo nano /etc/mysql/mariadb.conf.d/50-server.cnf
# bind-address = 0.0.0.0 (또는 WEB 서버 IP 허용)
```

### Q: 권한 에러

```bash
sudo chown -R ubuntu:ubuntu /var/www/schemaSpy
```

---

## 9. 관련 문서

- [ERD 문서](architecture/erd.md)
- [SQL DDL 문서](architecture/sql-ddl.md)
- [개발 가이드](development-guide.md)
