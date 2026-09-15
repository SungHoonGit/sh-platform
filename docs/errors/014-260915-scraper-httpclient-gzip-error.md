# 014-260915-scraper-httpclient-gzip-error 오류 기록

## 개요
- **발생일**: 2026-09-15 (사람인 수집은 약 2026-08-31부터 중단으로 확인)
- **환경**: scraper 백엔드 (Spring Boot 3.4.4, Java 21), 프로덕션 서버(oci-web)
- **심각도**: 🔴 Critical (통합검색 사람인 실패 + 사람인 크론 수집 8/31 이후 0건)

## 1. 오류 현상
### 1.1 에러 메시지
가. 통합검색(scraper SearchService) — 사람인 실패:
```
ERROR c.s.platform.service.SearchService - Failed to get results from site: saramin
java.util.concurrent.ExecutionException: java.lang.RuntimeException: Failed to crawl saramin
```
나. 크론 수집 (사람인) — 응답 파싱 실패:
```
ERROR c.s.p.service.CrawlExecutionService - Failed to crawl site: saramin
com.fasterxml.jackson.core.JsonParseException: Illegal character ((CTRL-CHAR, code 31)): only regular white space (\r, \n, \t) is allowed between tokens
```
다. (배포 창 일시) 잡코리아 — `java.util.concurrent.TimeoutException` (30초)

### 1.2 관찰된 증상
- 뷰어에서 사람인이 **8/31 이후 하루도 수집되지 않음** (crawl_log 0건 수준).
- 사람인 `get-recruit-list` 응답에 `content-encoding: gzip` 헤더 + gzip 바이너리 바디가 옴을 재현.
- 잡코리아는 로컬 재현에서 정상 200/0.5s (배포 중 CPU 경합에 의한 일시 타임아웃).

## 2. 원인 분석
### 2.1 근본 원인 — HttpClient 전환(1b18ce9) 시 gzip 해제 누락
- 2026-08-28 커밋 `1b18ce9`에서 외부 curl(`curl -s -L --compressed`) → Java `java.net.http.HttpClient`로 전환(오류 기록 013, JDK in-place 업데이트 spawn helper 손상).
- 전환 시 `curl --compressed`의 gzip 자동 해제 기능을 **재현하지 않음**:
  `BodyHandlers.ofString()`은 Java HttpClient의 기본 동작으로 **압축 해제를 하지 않음**.
  (HttpClient가 `Accept-Encoding`을 우리가 넣어도 자동 해제하지 않는다.)
- 그런데도 요청 헤더에 `Accept-Encoding: gzip, deflate, br`을 그대로 유지 → 사람인 서버가 gzip 응답을 반환하면
  바이너리(코드 31 제어문자 다량 포함)가 그대로 문자열로 들어옴 → `ObjectMapper.readTree` 파싱 예외.
- 8/28~30은 사람인이 평문으로 응답해 동작했고, **약 8/31부터 gzip 응답을 시작하면서 중단**된 것으로 추정.

### 2.2 관련 코드 (변경 전)
- 파일: `crawler/SaraminCrawler.java`, (동일 버그 보유) `crawler/WantedCrawler.java`
- 코드:
```java
.header("Accept-Encoding", "gzip, deflate, br")   // 광고만 했지
...
.send(request, HttpResponse.BodyHandlers.ofString()); // 해제는 없음
```
- `curl --compressed`(변경 전) 가 동작했던 이유: curl이 Content-Encoding을 보고 스스로 해제.

## 3. 해결 방법
### 3.1 해결 과정
공통 유틸 `crawler/HttpFetcher.java` 신설.
- `BodyHandlers.ofByteArray()`로 수신 후 `Content-Encoding` 헤더에 따라 **gzip/x-gzip, deflate 자동 해제**.
- 광고 인코딩에서 `br`을 제거(`Accept-Encoding: gzip, deflate`) — JDK 표준에 brotli 디코더가 없으므로 광고 시 해제 불가.
- `SaraminCrawler`, `WantedCrawler`의 `fetchWithCurl`을 `HttpFetcher.send` + `HttpFetcher.decodeBody`로 교체.
  (기존 로직과 동등: 30초 타임아웃, redirect NORMAL, >=400/!=200 상태 처리 유지)
- 잡코리아/원티드/리멤버는 `Accept-Encoding`을 보내지 않아 평문 응답이라 영향 없음 — 원티드는 같은 잠재 버그가 있어 동일 수정, 잡코리아는 배포 창 일시 타임아웃으로 판단.

### 3.2 최종 코드 변경
```java
// HttpFetcher.java (신규)
HttpResponse<byte[]> resp = HttpFetcher.send(request);   // ofByteArray + redirect NORMAL
return HttpFetcher.decodeBody(resp);                      // gzip/deflate 해제 후 UTF-8
```

### 3.3 테스트
`HttpFetcherTest` 5건: gzip 해제, GZip/x-gzip 별칭, deflate 해제, 비압축 원본, null 바디.

## 4. 예방 방법
- **`Accept-Encoding`에 gzip/deflate를 광고했으면 반드시 해제 로직을 두어야 한다.** (curl --compressed처럼)
  JDK HttpClient는 자동 해제하지 않는다.
- 신규 HTTP 호출 작성 시 규칙: `grep -rn 'Accept-Encoding' modules/scraper/backend/src` 로 광고를 검사하고,
  광고했으면 `HttpFetcher`를 사용할 것. brotli(`br`)는 광고 금지.
- 크론 실패·통합검색 실패 탐지: 응답 파싱 예외(`CTRL-CHAR`, `JsonParseException`)가 보이면
  먼저 응답 `Content-Encoding` 헤더 확인.

## 5. 참고 자료
- 수정 커밋: 015 참조 (일지 커밋 테이블)
- 선행 오류 기록: `013-260827-scraper-curl-jdk-crawl-failure-error.md` (curl→HttpClient 전환 맥락)
- 재현: `curl -s -D - <사람인 get-recruit-list URL>` 로 `content-encoding: gzip` 확인; `--compressed` 추가 시 정상 JSON.

---
*작성일: 2026-09-15*