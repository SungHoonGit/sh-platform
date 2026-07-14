# Scraper Platform 문서

> 스크래퍼 데이터 관리 및 모니터링 플랫폼

---

## 문서 구조

```
docs/
├── architecture.md     # 아키텍처 설계
└── roadmap.md         # 로드맵
```

---

## 데이터 경로 규칙

### 파일 저장 위치

```
/home/ubuntu/data/
├── scraper/                ← 스크래퍼 수집 데이터
│   ├── java/
│   │   ├── 2026-07-14.md
│   │   └── ...
│   └── react/
│       └── ...
└── shared/                 ← 공유 데이터 (선택)
```

### 경로 설정

```yaml
# application.yml
app:
  data:
    base-path: /home/ubuntu/data
    scraper:
      path: /home/ubuntu/data/scraper
```

### 이유

- **데이터 관리 용이**: 한 곳에서 모든 데이터 확인 가능
- **백업 간단**: `/home/ubuntu/data/` 하나만 백업
- **확장 용이**: 새 애플리케이션 추가 시 하위 폴더만 생성
- **안전성**: 배포 시 데이터 덮어쓰기 방지

---

## 아키텍처 요약

| 항목 | 내용 |
|------|------|
| 백엔드 | Spring Boot 3.5.x |
| 프론트엔드 | React, TypeScript |
| DB | MariaDB |
| 포트 | :8081 (API), :3001 (UI) |

---

## 구현 단계

| Phase | 내용 | 기간 |
|-------|------|------|
| Phase 1 | 기본 구조 | 1주 |
| Phase 2 | 데이터 뷰어 | 1주 |
| Phase 3 | 스크래퍼 설정 | 1주 |
| Phase 4 | 알림 설정 | 1주 |
| Phase 5 | 모니터링 | 1주 |

---

## API 엔드포인트

### 데이터 뷰어
- GET /api/v1/files - 파일 트리
- GET /api/v1/files/{path} - 파일 내용
- GET /api/v1/files/search - 검색

### 스크래퍼 설정
- GET /api/v1/scraper/configs - 설정 목록
- POST /api/v1/scraper/configs - 설정 생성
- POST /api/v1/scraper/configs/{id}/run - 수동 실행

### 알림 설정
- GET /api/v1/notifications - 알림 목록
- POST /api/v1/notifications/test - 테스트 발송

---

## 관련 프로젝트

| 프로젝트 | 위치 | 설명 |
|----------|------|------|
| sh-platform | /home/ubuntu/sh-platform | 메인 플랫폼 |
| scraper-platform | /home/ubuntu/scraper-platform | 스크래퍼 관리 |
| job-scraper | /home/ubuntu/job-scraper | 기존 스크래퍼 |
