# Scraper Backend

채용공고 수집 스크래퍼 백엔드 서비스 (port 8081)

## 개요

- 사람인, 잡코리아 등 채용 사이트에서 공고 수집
- 통합 검색 (키워드, 지역, 경력 필터)
- 스케줄러 (cron 기반 자동 수집)
- 이메일/브라우저 푸쉬 알림

## 기술 스택

- Java 21 LTS
- Spring Boot 3.4.4
- MariaDB 10.11.14
- Spring Data JPA
- Spring Security + JWT

## 구조

```
backend/
├── src/main/java/com/scraper/platform/
│   ├── api/                    # API DTO
│   ├── config/                 # 설정 (Security, CORS 등)
│   ├── controller/             # REST 컨트롤러
│   ├── crawler/                # 크롤러 (사람인, 잡코리아)
│   ├── model/                  # 엔티티
│   ├── repository/             # Repository
│   └── service/                # 비즈니스 로직
├── src/main/resources/
│   └── application.yml         # 설정
└── build.gradle.kts
```

## API 엔드포인트

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/scraper/docs/crawlers` | 스케줄 목록 |
| POST | `/scraper/docs/crawlers` | 스케줄 생성 |
| PUT | `/scraper/docs/crawlers/{id}` | 스케줄 수정 |
| DELETE | `/scraper/docs/crawlers/{id}` | 스케줄 삭제 |
| POST | `/scraper/docs/crawlers/{id}/execute` | 즉시 실행 |
| GET | `/scraper/docs/search` | 통합 검색 |
| GET | `/scraper/docs/view` | 뷰어 페이지 |
| GET | `/scraper/swagger-ui/` | Swagger UI |

## DB 테이블

| 테이블 | 설명 |
|--------|------|
| crawl_config | 스케줄 설정 (이름, 키워드, 크론, 알림 설정) |
| crawl_site_config | 사이트별 설정 (사람인, 잡코리아) |
| job_postings | 수집된 채용 공고 |
| crawl_logs | 실행 이력 |
| push_subscription | 브라우저 푸쉬 구독 |

## 환경 변수 (.env)

```bash
# DB
SPRING_DATASOURCE_URL=jdbc:mariadb://10.0.0.39:3306/scraper_platform
SPRING_DATASOURCE_USERNAME=sh_user
SPRING_DATASOURCE_PASSWORD=SHpass1234!

# JWT
JWT_PUBLIC_KEY=...

# 이메일 (Gmail SMTP)
MAIL_USERNAME=ksa134652@gmail.com
MAIL_PASSWORD=...

# 웹 푸쉬 (VAPID)
WEBPUSH_VAPID_PUBLIC_KEY=...
WEBPUSH_VAPID_PRIVATE_KEY=...
```

## 로컬 실행

```bash
# 빌드
./gradlew :modules:scraper:backend:build

# 실행
java -jar modules/scraper/backend/build/libs/sh-platform-scraper-*.jar

# 테스트
./gradlew :modules:scraper:backend:test
```

## 배포

GitHub Actions에서 master push 시 자동 배포:

1. Gradle 빌드
2. 프론트엔드 빌드 및 JAR에 포함
3. 서버에 JAR 복사 (`builds/sh-platform-scraper.jar`)
4. systemd 재시작

```bash
# 수동 배포
sudo systemctl restart sh-platform-scraper

# 로그 확인
sudo journalctl -u sh-platform-scraper --since "5 min ago" -f
```

## 문제 해결

| 문제 | 해결 |
|------|------|
| 포트 충돌 | `sudo fuser -k 8081/tcp` |
| DB 연결 실패 | `10.0.0.39` MariaDB 상태 확인 |
| 크롤 실패 | `crawl_logs` 테이블에서 에러 메시지 확인 |
| 푸쉬 알림 미수신 | Service Worker 상태 확인 (DevTools → Application) |
