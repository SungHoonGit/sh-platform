---
title: React Frontend Design
description: React Frontend Design - general module documentation
category: front
created: 2026-07-16
updated: 2026-07-21
---

# React 프론트엔드 설계 문서

## 1. 목표

- Spring Boot 백엔드 API를 소비하는 단일 페이지 애플리케이션(SPA)
- 채용공고 크롤링 시스템의 관리 및 조회 인터페이스
- 모바일 반응형 미지원 (데스크톱 전용)

## 2. 기술 스택

| 구분 | 기술 | 버전 |
|------|------|------|
| 프레임워크 | React | 19.x |
| 빌드 도구 | Vite | 8.x |
| 언어 | TypeScript | 6.x |
| CSS | Tailwind CSS | 4.x |
| 상태관리 | Zustand | 5.x |
| 데이터 페칭 | TanStack Query | 5.x |
| 라우팅 | React Router | 7.x |
| 아이콘 | Lucide React | latest |

## 3. 사용자 플로우

```
메인 대시보드
├── 크롤러 목록 조회
│   ├── 크롤러 선택 → 검색 설정 표시
│   ├── 크롤러 실행 → 상태 폴링 → 완료
│   └── 크롤러 수정 → (추후)
├── 파일 탐색
│   ├── 디렉토리 선택
│   └── 날짜별 파일 선택 → 뷰어로 이동
└── 뷰어
    ├── 사이트 탭 필터
    ├── 정렬 (회사명, 포지션, 마감일)
    ├── 검색 (텍스트 필터)
    └── 페이징
```

## 4. 페이지 설계

### 4.1 메인 대시보드 (`/`)

**레이아웃:**
```
┌─────────────────────────────────────────────────────┐
│  🤖 SH Platform          [로그인] [설정]             │
├──────────────┬──────────────────────────────────────┤
│              │                                      │
│  크롤러 목록  │        메인 콘텐츠 영역               │
│              │                                      │
│  ▶ java_daily│  ┌─────────────────────────────┐    │
│    검색설정  │  │  크롤러 상세 정보             │    │
│    ├ 사람인  │  │  - 이름: java_daily          │    │
│    ├ 잡코리아│  │  - 스케줄: 0 9 * * *         │    │
│    ├ 원티드  │  │  - 경로: /home/ubuntu/...    │    │
│    └ 리멤버  │  │  - 마지막 실행: 2026-07-16   │    │
│              │  └─────────────────────────────┘    │
│  ▶ react_day│                                      │
│    ...       │  ┌─────────────────────────────┐    │
│              │  │  최근 크롤링 결과             │    │
│              │  │  - 2026-07-16: 360건        │    │
│              │  │  - 2026-07-15: 290건        │    │
│              │  └─────────────────────────────┘    │
│              │                                      │
├──────────────┴──────────────────────────────────────┤
│  © 2026 SH Platform                                 │
└─────────────────────────────────────────────────────┘
```

**기능:**
- 크롤러 선택 시 오른쪽에 상세 정보 표시
- 검색 설정 (사이트별 키워드, 경력, 지역)
- 마지막 크롤링 결과 요약
- 크롤러 실행 버튼

### 4.2 파일 탐색 (`/files`)

**레이아웃:**
```
┌─────────────────────────────────────────────────────┐
│  📁 파일 탐색기                                      │
├──────────────┬──────────────────────────────────────┤
│              │                                      │
│  디렉토리    │        파일 내용 미리보기              │
│              │                                      │
│  📂 daily    │  ┌─────────────────────────────┐    │
│  ├ 📂 java   │  │  # 2026-07-16 Java 채용공고  │    │
│  │  ├ 07-16  │  │  > 총 360건                  │    │
│  │  ├ 07-15  │  │                              │    │
│  │  └ 07-14  │  │  ## 사람인 (150건)           │    │
│  └ 📂 react  │  │  ### (주)회사명              │    │
│              │  │  - 포지션: Java 개발자        │    │
│              │  │  - 경력: 3~5년               │    │
│              │  └─────────────────────────────┘    │
│              │                                      │
├──────────────┴──────────────────────────────────────┤
│  [뷰어로 열기] [내보내기]                             │
└─────────────────────────────────────────────────────┘
```

**기능:**
- 왼쪽: 디렉토리 트리 (펼치기/접기)
- 오른쪽: 선택된 파일 미리보기
- 하단: 뷰어로 열기 버튼

### 4.3 뷰어 (`/viewer`)

**레이아웃:**
```
┌─────────────────────────────────────────────────────┐
│  📄 2026-07-16 Java 채용공고          [내보내기 ▼]   │
├─────────────────────────────────────────────────────┤
│  [전체] [사람인 150] [잡코리아 60] [원티드 100]     │
│  [리멤버 150]                        🔍 검색: [___] │
├─────────────────────────────────────────────────────┤
│  회사명        │ 포지션        │ 경력  │ 기술  │ 마감│
│  ├────────────┼──────────────┼──────┼──────┼─────│
│  (주)마이링크  │ Java 개발PM   │ 15~20│ Java │ 내일│
│  (주)아이투맥스 │ Java Developer│ 2년↑ │ AWS  │ 08.06│
│  ...          │ ...          │ ...  │ ...  │ ... │
├─────────────────────────────────────────────────────┤
│  ◀ 1 2 3 ... 9 ▶                    총 360건      │
└─────────────────────────────────────────────────────┘
```

**기능:**
- 상단: 파일 제목 + 내보내기 (PDF/Excel)
- 탭: 사이트별 필터 + 건수 뱃지
- 테이블: 정렬 가능 열 헤더
- 검색: 실시간 텍스트 필터
- 하단: 페이징

### 4.4 크롤러 설정 (`/settings/crawlers`)

**레이아웃:**
```
┌─────────────────────────────────────────────────────┐
│  ⚙️ 크롤러 설정                                     │
├─────────────────────────────────────────────────────┤
│                                                     │
│  크롤러: java_daily                                  │
│  스케줄: [0 9 * * *] [수정]                          │
│  출력경로: [/home/ubuntu/job-scraper/daily/java]     │
│                                                     │
│  ┌─────────────────────────────────────────────┐   │
│  │ 사이트 설정                                   │   │
│  ├─────────────────────────────────────────────┤   │
│  │ ✅ 사람인                                     │   │
│  │    키워드: [Java 백엔드]                      │   │
│  │    경력: [3~5년]                              │   │
│  │    지역: [서울]                                │   │
│  │    직무: [개발]                                │   │
│  ├─────────────────────────────────────────────┤   │
│  │ ✅ 잡코리아                                   │   │
│  │    키워드: [Java Spring]                      │   │
│  │    경력: [3~5년]                              │   │
│  │    지역: [서울]                                │   │
│  ├─────────────────────────────────────────────┤   │
│  │ ✅ 원티드                                     │   │
│  │    키워드: [Java 백엔드]                      │   │
│  │    경력: [3~5년]                              │   │
│  │    지역: [서울]                                │   │
│  ├─────────────────────────────────────────────┤   │
│  │ ✅ 리멤버                                     │   │
│  │    키워드: [Java] (API 미지원)                │   │
│  │    경력: [3~5년]                              │   │
│  │    지역: [서울]                                │   │
│  └─────────────────────────────────────────────┘   │
│                                                     │
│  [저장] [취소]                                       │
│                                                     │
└─────────────────────────────────────────────────────┘
```

**기능:**
- 크롤러 기본 정보 수정
- 사이트별 검색 조건 편집
- 활성화/비활성화 토글
- 저장/취소

## 5. API 연동

### 5.1 사용 API 엔드포인트

| 기능 | Method | URL | 설명 |
|------|--------|-----|------|
| 크롤러 목록 | GET | `/docs/crawlers` | 크롤러 + 사이트 설정 |
| 디렉토리 목록 | GET | `/docs/list?rootPath=...` | 파일 트리 |
| 채용공고 조회 | GET | `/docs/jobs?rootPath=...&path=...&site=...` | 페이징 데이터 |
| 크롤러 실행 | POST | `/crawl-config/{id}/execute` | 비동기 실행 |
| 실행 상태 | GET | `/crawl-logs/config/{id}/recent` | 폴링용 |
| 크롤러 설정 | GET/PUT | `/crawl-config/{id}` | CRUD |

### 5.2 API 호출 예시

```typescript
// 크롤러 목록 조회
const { data: crawlers } = useQuery({
  queryKey: [crawlers],
  queryFn: () => fetch(/scraper/docs/crawlers).then(r => r.json())
});

// 채용공고 조회 (사이트 필터)
const { data: jobs } = useQuery({
  queryKey: [jobs, rootPath, path, site, page],
  queryFn: () => fetch(
    `/scraper/docs/jobs?rootPath=${rootPath}&path=${path}&site=${site}&page=${page}&size=20`
  ).then(r => r.json())
});

// 크롤러 실행 (낙관적 업데이트)
const executeMutation = useMutation({
  mutationFn: (configId) => fetch(`/scraper/crawl-config/${configId}/execute`, { method: POST }),
  onSuccess: () => queryClient.invalidateQueries({ queryKey: [crawlers] })
});
```

## 6. 컴포넌트 구조

```
src/
├── api/
│   └── scraper.ts          # API 클라이언트 함수
├── components/
│   ├── Layout.tsx           # 전체 레이아웃 (사이드바 + 메인)
│   ├── Sidebar.tsx          # 사이드바 (크롤러 목록)
│   ├── CrawlerCard.tsx      # 크롤러 카드 (정보 + 실행)
│   ├── SiteConfigForm.tsx   # 사이트 검색 조건 폼
│   ├── FileTree.tsx         # 디렉토리 트리
│   ├── JobGrid.tsx          # 채용공고 테이블
│   ├── SiteTabs.tsx         # 사이트 탭 필터
│   ├── Pagination.tsx       # 페이징
│   └── StatusBadge.tsx      # 상태 뱃지
├── pages/
│   ├── Dashboard.tsx        # 메인 대시보드
│   ├── Files.tsx            # 파일 탐색
│   ├── Viewer.tsx           # 뷰어
│   └── Settings.tsx         # 설정
├── stores/
│   └── useCrawlerStore.ts   # Zustand 스토어
├── hooks/
│   ├── useCrawlers.ts       # TanStack Query 훅
│   └── useJobs.ts
├── types/
│   └── index.ts             # TypeScript 타입
├── App.tsx
└── main.tsx
```

## 7. 상태 관리

### 7.1 Zustand 스토어

```typescript
interface CrawlerState {
  selectedCrawler: Crawler | null;
  selectedFile: string | null;
  currentSite: string;
  currentPage: number;
  
  setSelectedCrawler: (crawler: Crawler) => void;
  setSelectedFile: (path: string) => void;
  setCurrentSite: (site: string) => void;
  setCurrentPage: (page: number) => void;
}
```

### 7.2 TanStack Query 키

```typescript
queryKeys = {
  crawlers: [crawlers],
  crawler: (id) => [crawlers, id],
  files: (rootPath) => [files, rootPath],
  jobs: (rootPath, path, site, page) => [jobs, rootPath, path, site, page],
  crawlLogs: (configId) => [crawlLogs, configId],
}
```

## 8. 에러 처리

| 상황 | 처리 |
|------|------|
| API 호출 실패 | 토스트 알림 + 재시도 버튼 |
| 크롤러 실행 실패 | 에러 메시지 표시 + 로그 확인 링크 |
| 네트워크 불안정 | 자동 재시도 (TanStack Query) |
| 인증 실패 | 로그인 페이지로 리다이렉트 |

## 9. 성능 고려사항

- **캐싱**: TanStack Query 기본 캐싱 (5분)
- **폴링**: 크롤러 실행 시 3초 간격 폴링
- **지연 로딩**: 뷰어 데이터는 파일 선택 시 로드
- **가상 스크롤**: 1000건 이상일 때 적용 (추후)

## 10. 배포

### 10.1 빌드

```bash
cd frontend/web
npm install
npm run build  # → dist/ 폴더 생성
```

### 10.2 nginx 설정

```nginx
# React 빌드 결과 서빙
location / {
    root /home/ubuntu/sh-platform/frontend/web/dist;
    try_files $uri $uri/ /index.html;
}

# API 프록시 (기존 유지)
location /scraper/ {
    proxy_pass http://127.0.0.1:8081/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
}
```

### 10.3 systemd 서비스 (선택)

```ini
[Service]
ExecStart=/usr/bin/npm run dev --prefix /home/ubuntu/sh-platform/frontend/web
# 또는 빌드 후 정적 파일 서빙
```

## 11. 개발 순서

1. **1주차**: 프로젝트 세팅 + Tailwind + 라우팅
2. **2주차**: API 클라이언트 + 대시보드
3. **3주차**: 파일 탐색 + 뷰어
4. **4주차**: 설정 페이지 + 빌드/배포

## 12. 검토 사항

- [ ] CORS 설정 확인 (이미 허용됨)
- [ ] API 프록시 경로 확인
- [ ] 인증/인가 방식 결정 (현재 없음 → 추후 추가)
- [ ] 모바일 반응형 여부 (데스크톱 전용 권장)
- [ ] 다크모드 지원 (추후)
