---
title: Architecture
description: Architecture - scraper module documentation
category: scraper
created: 2026-07-14
updated: 2026-07-21
---

# Scraper Management Platform - 아키텍처 설계

> 스크래퍼 데이터 관리 및 모니터링 플랫폼
> 작성일: 2026-07-14

---

## 1. 개요

### 1.1 목적

- 스크래퍼로 수집된 MD 파일 관리 및 조회
- 스크래퍼 설정 및 스케줄링 관리
- 알림 설정 (이메일, 카카오톡)
- API 연동 상태 모니터링

### 1.2 기술 스택

| 항목 | 기술 |
|------|------|
| 백엔드 | Spring Boot 3.5.x, Java 21 |
| 프론트엔드 | React, TypeScript, Vite |
| DB | MariaDB 10.11 |
| 파일 저장 | 로컬 파일시스템 (MD 파일) |
| 배포 | 직접 배포 |

---

## 2. 시스템 아키텍처

```
Client (Web Browser)
    |
    v
Scraper Platform API (Spring Boot :8081)
    |
    +---> File Service ---> File System (MD files)
    |
    +---> Scraper Service ---> DB (MariaDB)
    |
    +---> Notification Service ---> Email / KakaoTalk
```

---

## 3. 데이터 모델

### 3.1 스크래퍼 설정 (scraper_config)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | 설정 ID |
| name | VARCHAR(100) | 스크래퍼 이름 |
| type | VARCHAR(50) | 타입 (java/react) |
| search_conditions | JSON | 검색 조건 |
| schedule_cron | VARCHAR(50) | 스케줄 (cron) |
| is_active | BOOLEAN | 활성화 여부 |
| created_at | TIMESTAMP | 생성일 |
| updated_at | TIMESTAMP | 수정일 |

### 3.2 스크래퍼 실행 이력 (scraper_history)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | 이력 ID |
| config_id | BIGINT FK | 설정 ID |
| status | ENUM | 상태 (SUCCESS/FAILED/RUNNING) |
| started_at | TIMESTAMP | 시작 시간 |
| finished_at | TIMESTAMP | 종료 시간 |
| result_count | INT | 수집 건수 |
| error_message | TEXT | 에러 메시지 |

### 3.3 수집 데이터 메타 (scraped_data)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | 데이터 ID |
| config_id | BIGINT FK | 설정 ID |
| file_path | VARCHAR(500) | 파일 경로 |
| file_name | VARCHAR(100) | 파일 이름 |
| scraped_at | DATE | 수집 날짜 |
| job_count | INT | 채용 공고 수 |
| created_at | TIMESTAMP | 생성일 |

### 3.4 알림 설정 (notification_config)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | 설정 ID |
| config_id | BIGINT FK | 스크래퍼 설정 ID |
| type | ENUM | 타입 (EMAIL/KAKAO) |
| target | VARCHAR(200) | 이메일 또는 카톡 ID |
| on_success | BOOLEAN | 성공 시 알림 |
| on_failure | BOOLEAN | 실패 시 알림 |
| is_active | BOOLEAN | 활성화 여부 |

---

## 4. API 엔드포인트

### 4.1 데이터 뷰어

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | /api/v1/files | 파일 트리 조회 |
| GET | /api/v1/files/{path} | 파일 내용 조회 |
| GET | /api/v1/files/search | 파일 검색 |
| GET | /api/v1/data | 수집 데이터 목록 |

### 4.2 스크래퍼 설정

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | /api/v1/scraper/configs | 설정 목록 |
| POST | /api/v1/scraper/configs | 설정 생성 |
| PUT | /api/v1/scraper/configs/{id} | 설정 수정 |
| DELETE | /api/v1/scraper/configs/{id} | 설정 삭제 |
| POST | /api/v1/scraper/configs/{id}/run | 수동 실행 |
| GET | /api/v1/scraper/history | 실행 이력 |

### 4.3 알림 설정

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | /api/v1/notifications | 알림 설정 목록 |
| POST | /api/v1/notifications | 알림 설정 추가 |
| PUT | /api/v1/notifications/{id} | 알림 설정 수정 |
| DELETE | /api/v1/notifications/{id} | 알림 설정 삭제 |
| POST | /api/v1/notifications/test | 테스트 발송 |

### 4.4 모니터링

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | /api/v1/monitor/status | 전체 상태 |
| GET | /api/v1/monitor/api | API 연동 상태 |

---

## 5. 프론트엔드 구성

### 5.1 메인 레이아웃

```
+------------------------------------------+
|  Header (로고, 사용자 정보)               |
+----------+-------------------------------+
|          |                               |
|  Sidebar |      Main Content             |
|  - 파일  |  +-------------------------+  |
|  - 설정  |  |                         |  |
|  - 알림  |  |   각 페이지별 컨텐츠     |  |
|  - 모니터|  |                         |  |
|          |  +-------------------------+  |
+----------+-------------------------------+
```

### 5.2 데이터 뷰어 페이지

```
+------------------------------------------+
|  검색: [________________] [검색]         |
+----------+-------------------------------+
|  파일    |  +-------------------------+  |
|  트리    |  |  마크다운 렌더링          |  |
|          |  |                         |  |
|  java    |  |  # 2026-07-14 Java 공고  |  |
|  +- 07-14|  |                         |  |
|  +- 07-13|  |  ## 회사A               |  |
|  +- 07-12|  |  - 포지션: 백엔드        |  |
|          |  |  - 기술: Java, Spring   |  |
|  react   |  +-------------------------+  |
|  +- ...  |                               |
+----------+-------------------------------+
```

---

## 6. 구현 단계

### Phase 1: 기본 구조 (1주)

- 프로젝트 생성 (Spring Boot + React)
- DB 스키마 생성
- 파일 읽기 API
- 프론트엔드 기본 레이아웃

### Phase 2: 데이터 뷰어 (1주)

- 파일 트리 API
- 마크다운 렌더링
- 검색 기능

### Phase 3: 스크래퍼 설정 (1주)

- 설정 CRUD
- 스케줄러 설정
- 수동 실행

### Phase 4: 알림 설정 (1주)

- 알림 설정 CRUD
- 이메일 발송
- 카카오톡 발송

### Phase 5: 모니터링 (1주)

- 상태 대시보드
- API 연동 상태
- 실행 이력

---

## 7. 배포

### 7.1 서버 구성

```
WEB 서버 (기존)
+-- nginx (프록시)
+-- Spring Boot (:8081)
+-- React (:3001)

DB 서버 (기존)
+-- MariaDB (scraper_platform DB 추가)
```

### 7.2 nginx 설정

```
location /scraper/ {
    proxy_pass http://localhost:8081/;
}
location /scraper-ui/ {
    root /home/ubuntu/scraper-platform/frontend/build;
    try_files $uri $uri/ /scraper-ui/index.html;
}
```
