# 현대 웹 서비스의 데이터 조회 방식과 크롤링 전략

## 들어가며

예전에는 웹 크롤링이라고 하면 대부분 **HTML을 다운로드한 후 원하는 데이터를 파싱하는 작업**을 의미했다.

하지만 React, Vue, Angular와 같은 SPA(Single Page Application)가 보편화되면서 웹의 데이터 조회 방식도 크게 변화하였다. 현재는 단순 HTML 크롤링보다 **브라우저가 내부적으로 호출하는 API(JSON)** 를 활용하는 방식이 훨씬 일반적이다.

> **요즘 크롤링은 HTML을 읽는 것이 아니라, 브라우저가 사용하는 데이터를 그대로 가져오는 방식으로 발전하고 있다.**

---

## 웹 페이지의 두 가지 렌더링 방식

웹 서비스는 크게 두 가지 방식으로 데이터를 제공한다.

### 1. 서버사이드 렌더링 (SSR) — 서버가 HTML을 완성해서 내려주는 방식

```
브라우저  ──GET /board──→  Spring Boot  ──→  DB 조회  ──→  HTML 생성  ──→  브라우저
```

HTML 안에 모든 데이터가 포함되어 있다. 서버가 `JSP`, `Thymeleaf`, `PHP`, `ASP.NET MVC` 등으로 템플릿을 렌더링하여 응답한다.

```html
<tr>
    <td>Java 개발자</td>
</tr>
```

이런 사이트는 **Jsoup(Java)** 또는 **BeautifulSoup(Python)** 만으로도 데이터를 쉽게 수집할 수 있다.

### 2. 클라이언트사이드 렌더링 (CSR) — 브라우저가 API를 호출하여 화면을 만드는 방식

```
브라우저  ──→  index.html (빈 껍데기)
                │
            React 실행
                │
            API 호출  ──→  JSON 수신  ──→  HTML 생성
```

처음 내려오는 HTML은 `React`나 `Vue`의 진입점 역할만 한다.

```html
<div id="root"></div>
```

실제 데이터는 브라우저가 JavaScript를 실행하면서 API를 호출하여 가져온다.

---

## React는 데이터를 어떻게 가져올까?

브라우저는 페이지 URL(`/jobs`)을 직접 요청하는 것이 아니라, 내부적으로 API(`/api/jobs?page=1`)를 호출한다.

요청:
```
GET /api/jobs?page=1
```

응답:
```json
{
  "jobs": [
    { "title": "Java 개발자", "company": "ABC" }
  ]
}
```

React는 이 JSON을 받아 화면을 동적으로 생성한다.

즉, 브라우저가 HTML을 통째로 다운로드하는 것이 아니라 **데이터(JSON)를 받아 클라이언트에서 HTML을 조립하는 구조**이다.

---

## Hidden API (Internal API)

공식 문서는 없지만 브라우저가 내부적으로 호출하는 API를 **Hidden API** 또는 **Internal API**라고 부른다.

Chrome 개발자도구에서 확인할 수 있다.

```
F12 → Network → Fetch / XHR
```

실제 예시:
- `GET /api/jobs`
- `GET /api/search`
- `POST /graphql`

공식 API는 아니지만 브라우저가 사용하기 때문에 누구나 쉽게 확인할 수 있으며, 크롤링 대상이 되기도 한다.

---

## HTML 크롤링과 JSON API 조회의 차이

| 항목 | HTML 크롤링 | JSON API 조회 |
|------|-----------|--------------|
| 전송 데이터 | HTML (태그+데이터) | JSON (순수 데이터) |
| 파싱 도구 | Jsoup, BeautifulSoup | HttpClient, RestTemplate, WebClient, OkHttp, Axios |
| 변환 작업 | HTML 분석 → 문자열 추출 | JSON → 객체 매핑 (ObjectMapper 등) |
| 안정성 | 구조 변경 시 깨지기 쉬움 | 필드명만 유지되면 안정적 |

HTML을 분석하는 것이 아니라 **객체(JSON)를 그대로 가져오는 방식**이 훨씬 효율적이다.

---

## Jsoup가 잘 동작하지 않는 이유

Jsoup는 **HTML 파싱만 수행**하며 JavaScript는 실행하지 않는다. React 사이트에서 Jsoup로 요청하면 `<div id="root"></div>`만 가져오는 경우가 많다. 실제 데이터는 브라우저가 JavaScript를 실행하면서 API를 호출한 후에야 생성되기 때문이다.

---

## Playwright / Selenium의 역할

Playwright는 실제 브라우저를 자동으로 실행한다.

```
브라우저 실행 → React 실행 → API 호출 → 렌더링 완료 → HTML 추출
```

사람이 브라우저를 조작하는 것과 동일한 환경을 제공하므로 React, Vue 기반 사이트도 정상적으로 수집할 수 있다.

**단점**: 브라우저를 띄우기 때문에 리소스 사용량이 크고 속도가 느리다.

---

## 실제 크롤링 전략 (우선순위)

현업에서는 일반적으로 다음 순서로 접근한다.

```
 1. 공식 API 확인
 2. ↓ (없으면)
 3. Hidden API(JSON) 확인
 4. ↓ (없으면)
 5. Playwright / Selenium
 6. ↓ (마지막)
 7. HTML Parsing (Jsoup 등)
```

### 1순위: 공식 API
- 가장 안정적이고 유지보수가 쉽다
- 속도가 빠르다
- 예: 원티드 공식 API, 사람인 오픈API

### 2순위: Hidden API (JSON)
- HTML 파싱이 불필요하다
- 객체로 바로 변환 가능하다
- 성능이 우수하다
- 예: 브라우저 개발자도구에서 확인되는 `/api/*` 엔드포인트

### 3순위: Playwright / Selenium
- JavaScript 렌더링이 필수인 사이트에 대응 가능하다
- 브라우저 실행으로 리소스 사용량이 증가한다

### 4순위: HTML Parsing (Jsoup 등)
- 단순 SSR 사이트에서는 매우 빠르고 효과적이다
- JavaScript 기반 사이트에서는 사용이 어렵다
- HTML 구조 변경에 취약하다

| 사이트 유형 | 추천 방식 |
|-----------|---------|
| 공식 API 제공 | 공식 API 사용 |
| React / Vue / Angular | Hidden API (JSON) 조회 |
| JavaScript 렌더링 필수 | Playwright |
| 서버에서 HTML 생성 (SSR) | Jsoup / HTML Parsing |

---

## Spring Boot 구현 구조

```
React (Frontend)
    │
    ↓
Spring Boot (Backend)
    │
    ↓
JobSearchService (통합 인터페이스)
    │
    ├── WantedApiClient
    ├── SaraminApiClient
    ├── JobKoreaApiClient
    └── RememberApiClient
    │
    ↓
통합 DTO → REST API 응답
```

각 사이트별로 조회 방식은 달라도(DTO 변환, 파라미터 매핑) 최종적으로는 동일한 DTO를 반환하도록 설계한다.

---

## 핵심 정리

- **Jsoup는 HTML Parser**이며 JavaScript를 실행하지 않는다.
- React, Vue, Angular 기반 사이트는 대부분 **JSON API**를 통해 데이터를 가져온다.
- 개발자도구(Network → Fetch/XHR)를 확인하면 실제 데이터 조회 방식을 파악할 수 있다.
- 가능하면 **공식 API**를 사용하고, 없다면 **Hidden API (JSON)** 를 활용한다.
- API도 없고 JavaScript 렌더링만 가능한 경우 **Playwright**와 같은 브라우저 자동화 도구를 사용한다.
- HTML 크롤링은 여전히 유효한 방법이지만, 현대 웹에서는 **가장 마지막에 고려하는 전략**인 경우가 많다.
- "크롤링"의 의미도 HTML 파싱에서 **웹 애플리케이션이 사용하는 데이터를 효율적으로 수집하는 방향**으로 변화하고 있다.
