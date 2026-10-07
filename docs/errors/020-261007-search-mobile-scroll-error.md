# 020-261007-search-mobile-scroll-error 오류 기록

## 개요
- **발생일**: 2026-10-07
- **환경**: 모바일 브라우저 (390×844), 운영 서버
- **심각도**: 🟡 Warning (모바일 통합검색 사용 불가 수준)

## 1. 오류 현상
### 1.1 증상
- 모바일에서 통합검색(`/scraper/`) 필터가 사이트 "점핏" 근처에서 잘려 보이고
- 하단의 **검색·스케줄 등록 버튼이 안 보이며, 화면을 스크롤해도 안 나옴**

### 1.2 재현 단계
1. 모바일(또는 390px 뷰포트)에서 `/scraper/` 접속
2. 필터가 뷰포트 하단에서 끊김
3. 위로 끌어도(스크롤) 페이지가 움직이지 않음

## 2. 원인 분석
### 2.1 근본 원인
scraper Layout이 AppShell의 `main`에 `overflow-hidden`을 지정해 **main 스크롤이 차단**됨.

- 데스크톱: Search 루트가 `md:h-full`로 높이가 main에 딱 맞아 드러나지 않음
- 모바일: Search 루트가 `h-auto` → 콘텐츠 1001px > main 756px → **245px가 클립 + 스크롤 불가**

### 2.2 관련 코드
- 파일: `modules/scraper/frontend/src/components/Layout.tsx:27`
- 코드: `mainClassName="flex-1 overflow-hidden"`

실측 (Chrome headless 390×844):
```
main: scrollHeight=1001, clientHeight=756, overflow="hidden"  ← 원인
검색 버튼: top=868 (뷰포트 844 밖)
```

## 3. 해결 방법
```diff
- mainClassName="flex-1 overflow-hidden"
+ mainClassName="flex-1 overflow-auto"
```
- 모바일: main 스크롤로 필터 전체·버튼 접근 가능
- 데스크톱: `h-full`로 콘텐츠가 main에 맞아 스크롤바 미생성 → 영향 없음
- 커밋: `604f467` (deploy 37586346732)

### 검증
Chrome headless(puppeteer-core + 시스템 Chrome)로 로그인 → 390×844 캡처:
`main.overflow="auto"`, 스크롤 후 검색 버튼 top=623(뷰포트 내) — 버튼 육안 확인.

## 4. 예방 방법
- `h-auto`로 콘텐츠가 자라나는 페이지에 `overflow-hidden` 컨테이너를 쓰면 잘림·스크롤 차단 발생
- 모바일 반응형 페이지 추가 시 main 컨테이너 overflow 정책 항상 확인

## 5. 참고 자료
- 원인 분석: puppeteer DOM 실측 (`_repro.cjs`, 검증 후 삭제)
- 관련: `docs/daily/2026-10-07-work-log.md`

---
*작성일: 2026-10-07*
