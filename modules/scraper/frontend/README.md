# Scraper Frontend

React + TypeScript + Vite 스크래퍼 SPA

## 개요

- 채용공고 통합 검색
- 스케줄 관리 (생성/수정/삭제/즉시 실행)
- 실시간 크롤 진행 상태 (WebSocket)
- 이메일/브라우저 푸쉬 알림 설정

## 기술 스택

- React 19
- Vite 8
- TypeScript 6
- Tailwind CSS 4
- React Query (TanStack)

## 시작

```bash
cd modules/scraper/frontend
npm install
npm run dev
```

## 구조

```
frontend/
├── public/
│   └── sw.js                 # Service Worker (웹 푸쉬)
├── src/
│   ├── api/                  # API 클라이언트
│   ├── components/           # 공통 컴포넌트
│   ├── contexts/             # React Context (크롤 진행 상태)
│   ├── hooks/                # 커스텀 훅
│   │   ├── useAuth.ts        # 인증
│   │   └── usePushNotification.ts  # 푸쉬 알림
│   ├── pages/                # 페이지
│   │   ├── Search.tsx        # 통합 검색
│   │   ├── Schedule.tsx      # 스케줄 관리
│   │   └── Viewer.tsx        # 채용공고 뷰어
│   ├── App.tsx               # 라우팅 + SW 등록
│   └── main.tsx              # 엔트리포인트
├── index.html
├── vite.config.ts
└── package.json
```

## 주요 기능

### 1. 통합 검색 (`/scraper/docs/search`)
- 키워드, 지역, 경력 필터
- 사이트별 필터 (사람인, 잡코리아)
- 결과 정렬 (최신순, 회사명순)

### 2. 스케줄 관리 (`/scraper/docs/schedule`)
- 크론 기반 자동 수집 설정
- 사이트별 파라미터 설정
- 이메일/푸쉬 알림 설정
- 즉시 실행

### 3. 뷰어 (`/scraper/docs/view`)
- 수집된 채용공고 목록
- 상세 정보 표시
- 외부 링크 연결

## API

- Base URL: `/scraper`
- Swagger: `/scraper/swagger-ui/`
- 인증: JWT (Authorization: Bearer {token})

## 배포

GitHub Actions에서 master push 시 자동 빌드 및 백엔드 JAR에 포함.
