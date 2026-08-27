# 011-260828-Viewer-TDZ-오류 기록

## 개요
- **발생일**: 2026-08-28
- **환경**: scraper 프론트 (React + Vite), 배포 후 운영 브라우저
- **심각도**: 🔴 Critical (Viewer 화면 완전 미렌더 — ErrorBoundary로 화면이 안 뜸)

## 1. 오류 현상
### 1.1 에러 메시지
```
Viewer 접근 시 (네트워크는 정상, GET favicon 401은 무관)
ReferenceError: Cannot access 'B' before initialization
    at index-*.js:43:9056
    at Array.filter (<anonymous>)
    at eo ...
[ErrorBoundary] ReferenceError ... {componentStack: ...}
```

### 1.2 재현 단계
1. https://sunghoonyk.duckdns.org/scraper/viewer 접속
2. 화면이 전혀 렌더링되지 않고 빈 화면 + 콘솔에 위 TDZ 오류
   - `GET /scraper/favicon.svg 401` 은 **본 오류와 무관** (파비콘 경로 미정의)

## 2. 원인 분석
### 2.1 근본 원인 — TDZ(Temporal Dead Zone) 크래시
`modules/scraper/frontend/src/pages/Viewer.tsx` 에서 변수 **선언 순서가 꼬인 것**이 원인.

```tsx
// (수정 전 — 잘못된 순서)
const displayJobs = (searchKeyword.trim() ? filteredJobs : jobs)
  .filter((j) => !blacklisted.has(normCompany(j.company))); // ← 여기서 blacklisted 참조
const displayTotal = ...;
const displayTotalPages = ...;

const [blacklisted, setBlacklisted] = useState<Set<string>>(new Set()); // ← 이 아래에서야 선언
```

`displayJobs` 의 `filter` 콜백이 **아직 선언되지 않은 `blacklisted`**(아래에서 `const` 선언)를 읽으므로,
JS 엔진이 `Cannot access 'blacklisted' before initialization` 을 던짐. 미니파이된 번들에선 변수명이 `B` 로 축약되어 위 메시지가 됨.

### 2.2 재발 원인
- 과거 커밋 `20a3469` 가 동일 TDZ를 이미 수정한 바 있음.
- 그러나 이후 리팩토링에서 `displayJobs`(블랙리스트 필터) 관련 코드가 `blacklisted` 상태 선언 **위로** 끌어올려지며 **재발**.
- `tsc` 는 "선언 전 사용"을 에러로 잡지 않음(타입상으론 유효하므로), 따라서 **로컬 타입체크만으로는 발견 불가** → 배포 후에야 런타임 오류로 드러남.

## 3. 해결 방법
### 3.1 해결 과정
블랙리스트 관련 상태/핸들러 블록(`blacklisted`, `showBl`, `blItems`, `loadBl`, `unblock`, `blockCompany`, `fetchBlacklist` useEffect)을
`displayJobs` 필터 **이전으로 이동**하여 선언 순서를 바로잡음.

### 3.2 최종 코드 변경
```tsx
// 블랙리스트 상태/핸들러를 displayJobs 보다 위로 이동
const filteredJobs = useMemo(() => { ... }, [jobs, searchKeyword]);

const [blacklisted, setBlacklisted] = useState<Set<string>>(new Set());
const [showBl, setShowBl] = useState(false);
const [blItems, setBlItems] = useState<BlacklistItem[]>([]);
const loadBl = () => { ... };
const unblock = async (item: BlacklistItem) => { ... };
const blockCompany = async (company: string) => { ... };

useEffect(() => { fetchBlacklist().then(...); }, []);

// displayJobs — 이제 blacklisted가 이미 선언됨
const displayJobs = (...).filter((j) => !blacklisted.has(normCompany(j.company)));
```

## 4. 예방 방법
- **상태(`useState`)는 컴포넌트 최상단에 모아 선언**하고, 파생값(`displayJobs`)은 반드시 그 아래에 둘 것.
- 파생값이 읽는 상태보다 위에서 파생값을 계산하지 않도록 주의.
- 이 유형(파생값 → 상태 순서)은 `tsc` 로 안 걸리므로, **PR 리뷰/자기 점검 시 선언 순서를 기준으로 체크**.
- 신규 데이터 흐름 추가 시 "선언 전 참조가 없는지" 확인.

## 5. 참고 자료
- 과거 유사 수정: `20a3469 fix: blacklisted/starred 상태 선언 순서 TDZ 크래시 수정`
- 수정 커밋: `4a6e1a9 fix: Viewer 블랙리스트 TDZ 크래시 — blacklisted 상태를 displayJobs 필터보다 위로 이동`

---
*작성일: 2026-08-28*
