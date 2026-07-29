# opencode 설정 가이드 (회사용)

## opencode란?
AI 기반 코딩 어시스턴트. CLI에서 자연어로 코딩 작업 요청 가능.

## 이 프로젝트에서의 사용법

### 기본 명령어
```
# 코드 검색
src 폴더에서 CrawlConfig 관련 코드 찾아줘

# 수정
CrawlConfig에 crawlerName 필드 추가해줘

# 빌드
./gradlew build -x test

# 배포
ssh oci-web에서 sh-platform-scraper 재시작해줘
```

### 프로젝트 구조 이해
opencode는 자동으로 프로젝트 구조를 파악합니다:
- `build.gradle.kts` -> Gradle 프로젝트
- `src/main/java/` -> 소스 코드
- `src/main/resources/` -> 설정 파일
- `docs/` -> 문서

### SSH 접속 설정
`~/.ssh/config`에 추가:
```
Host oci-web
    HostName 140.245.95.162
    User ubuntu
    IdentityFile ~/.ssh/oci/140.245.95.162/ssh-key-2026-07-11.key

Host oci-db
    HostName 140.245.95.162
    User ubuntu
    IdentityFile ~/.ssh/oci/140.245.95.162/ssh-key-2026-07-11.key
```

### 자주 사용하는 패턴

#### 1. 코드 수정 후 빌드
```bash
# 수정
opencode: "CrawlConfig에 description 필드 추가해줘"

# 빌드 확인
./gradlew :scraper-platform-backend:build -x test
```

#### 2. API 추가
```bash
opencode: "새로운 API 엔드포인트 추가해줘:
- GET /docs/crawlers
- 크롤러 목록 조회
- CrawlConfigRepository에서 활성 설정만 조회"
```

#### 3. DB 마이그레이션
```bash
opencode: "crawl_config 테이블에 column 추가 SQL 작성해줘"
```

#### 4. 문제 해결
```bash
opencode: "스크래퍼 서비스가 시작 안 돼. 로그 확인하고 문제 찾아줘"
```

### 주의사항
- **빌드**: 변경 후 반드시 `./gradlew build -x test` 실행
- **배포**: 서버에서 `sudo systemctl restart sh-platform-{service}`
- **DB**: root 접근은 `sudo mysql` 필요
- **git**: 변경 후 커밋 & 푸시 필요

---
최종 업데이트: 2026-07-15
