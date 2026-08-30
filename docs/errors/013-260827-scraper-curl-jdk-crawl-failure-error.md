# 013-260827-scraper-curl-jdk-crawl-failure 오류 기록

## 개요
- **발생일**: 2026-08-27 (0건 수집), 근본수정 2026-08-28
- **환경**: scraper 백엔드 (Spring Boot 3.4.4, Java 21), 프로덕션 서버(oci-web)
- **심각도**: 🔴 Critical (하루 종일 모든 크론 수집 실패 — `crawl_log` 27일 0건)

## 1. 오류 현상
### 1.1 에러 메시지
서버 로그에서 크론 실행 전부 실패:
```
java.io.IOException: Cannot run program "curl": Failed to exec spawn helper ...
    Ram Reading spy ...
    Restart JVM, especially after in-place JDK updates
```

### 1.2 재현 단계
1. 2026-08-27 09:00~18:00대, 모든 활성 크롤러(react 서울 2년차 / java All / react All / java 대전 / java 서울 경기) 스케줄 실행
2. 각 실행마다 위 spawn helper 예외가 발생해 수집 0건
3. 서비스 자체는 다운되지 않았음 (28일 01:27 재시작 전까지 실행 중)

## 2. 원인 분석
### 2.1 근본 원인 — 외부 `curl` 의존 + in-place JDK 업데이트
스크래퍼는 채용사이트(사람인/잡코리아/원티드) 크롤링과 기업 평점 수집(잡플래닛/잡코리아/사람인)을
`ProcessBuilder`로 **외부 `curl` 프로세스를 실행**하는 방식이었음 (4개 파일).

서버에서 `curl`이 위치한 JDK/시스템 런타임의 **in-place 업데이트**가 일어나며 JDK의 spawn helper
(`jspawnhelper`)가 손상/불일치 → `ProcessBuilder.start()`가 `Failed to exec spawn helper`를 던짐.
이 예외는 **모든 하위 프로세스 생성**에 전파되어, curl 기반 크롤링이 전부 실패.

### 2.2 관련 코드 (변경 전)
- 파일: `modules/scraper/backend/src/main/java/com/scraper/platform/crawler/SaraminCrawler.java`
- 코드: `ProcessBuilder pb = new ProcessBuilder("curl", "-s", "-L", ...); Process process = pb.start(); ...`
- 동일 패턴: `JobkoreaCrawler`(POST), `WantedCrawler`, `service/CompanyRatingService`(평점 3곳)

## 3. 해결 방법
### 3.1 해결 과정
외부 `curl` 의존을 제거하고 **Java 21 내장 `java.net.http.HttpClient`**로 전환.
- 하위 프로세스 생성(ProcessBuilder/curl) 제거 → JVM 내부에서 직접 HTTP 요청
- `curl -s -L --max-time 30 --compressed` 동작을 `.GET()/.POST()` + `.timeout(Duration.ofSeconds(30))` + `.followRedirects(NORMAL)` + 헤더로 재현
- 상태코드 처리: `>= 400`이면 `IOException` (Saramin/Jobkorea), 200만 성공 처리 (Wanted는 기존 `%{http_code}` 동작 유지)

### 3.2 최종 코드 변경
```java
// (변경 후 예시 — SaraminCrawler)
HttpRequest request = HttpRequest.newBuilder(URI.create(url))
    .header("User-Agent", "...")
    .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
    .header("Accept-Encoding", "gzip, deflate, br")
    .timeout(Duration.ofSeconds(30))
    .GET()
    .build();
HttpResponse<String> resp = HttpClient.newBuilder()
    .followRedirects(HttpClient.Redirect.NORMAL)
    .build()
    .send(request, HttpResponse.BodyHandlers.ofString());
if (resp.statusCode() >= 400) {
    throw new IOException("HTTP " + resp.statusCode() + " for URL: " + url);
}
return resp.body();
```

### 3.3 전환 파일 목록
| 파일 | 방식 |
|------|------|
| `crawler/SaraminCrawler.java` | GET + 헤더, 4xx/5xx → IOException |
| `crawler/JobkoreaCrawler.java` | POST + JSON body, 4xx/5xx → IOException |
| `crawler/WantedCrawler.java` | GET, 200만 성공(非200 → null, 기존 `%{http_code}` 동작 유지) |
| `service/CompanyRatingService.java` | 잡플래닛/잡코리아/사람인 3곳 GET, 평점 regex 추출 유지 |

## 4. 예방 방법
- **외부 바이너리(프로세스) 의존 크롤링/호출 금지** — 반드시 JVM 내장(또는 라이브러리) HTTP 클라이언트 사용.
  프로세스 생성은 OS/JDK 환경(in-place 업데이트, 경로)에 깨지기 쉽다.
- 크롤러 신규/수정 시 `curl`/`ProcessBuilder` 검색: `grep -rn 'ProcessBuilder\|"curl"' modules/scraper`.
- 크론 실패는 로그보다 `crawl_log`(0건)로 먼저 감지하고, spawn helper 예외가 보이면 서버 JDK 재시작 전에 프로세스 의존 여부부터 점검.

## 5. 참고 자료
- 수정 커밋: `1b18ce9 fix: 스크래퍼 외부 curl/ProcessBuilder → Java 내장 HttpClient 전환 (JDK in-place 업데이트로 인한 크롤 실패 해결)`
- 검증: `./gradlew :modules:scraper:backend:test` 성공, 배포 후 28일 09:00부터 정상 수집

---
*작성일: 2026-08-28*
