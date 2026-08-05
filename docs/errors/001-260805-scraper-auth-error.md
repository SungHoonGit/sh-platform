# 001-260805 scraper 수동 수집 오류 기록

## 개요
- **발생일**: 2026-08-05
- **환경**: Windows (로컬), Linux (서버), Java 21, Spring Boot 3.4.4
- **심각도**: 🔴 Critical (수동 수집 완전 불가)

## 1. 오류 현상

### 1.1 증상
- 프론트엔드에서 "수집 실행" 버튼 클릭 시
- 사람인(saramin)만 성공, 나머지 사이트 실패
- Toast에서 오류 메시지 표시

### 1.2 에러 메시지 (2건)

**오류 1: AuthorizationDeniedException**
```
org.springframework.security.authorization.AuthorizationDeniedException: Access Denied
```

**오류 2: Hibernate AssertionFailure**
```
org.hibernate.AssertionFailure: null id in com.scraper.platform.model.JobPosting entry
(don't flush the Session after an exception occurs)
```

### 1.3 재현 단계
1. 프론트엔드 로그인
2. 스케줄 페이지에서 "수집 실행" 버튼 클릭
3. 사람인만 성공, 나머지 사이트 실패

## 2. 원인 분석

### 2.1 오류 1: SSE progress 엔드포인트 차단
- SSE progress 엔드포인트(`/crawl-config/{id}/progress`)가 Spring Security에 의해 차단
- `EventSource`는 커스텀 헤더 전달 불가 → 쿼리 파라미터로 토큰 전달
- Spring Security가 쿼리 파라미터 토큰을 인식하지 못함

### 2.2 오류 2: Hibernate 세션 오염
- `executeCrawl`이 `@Transactional`로 한 트랜잭션에서 모든 사이트 처리
- 중복 공고 저장 시 `Duplicate entry` 에러 발생
- 이 에러로 Hibernate 세션이 오염됨
- 이후 모든 사이트 크롤링 실패 (`null id in JobPosting entry`)

### 2.3 추가 원인: CrawlConfig 기본값
- `CrawlConfig.java`에 `private Long accountId = 1L` (기본값)
- 로그인한 사용자의 `accountId = 4`
- `getConfigById(id, accountId)` → ID 불일치 → `NOT_FOUND`

## 3. 해결 방법

### 3.1 SecurityConfig 수정
```java
// SSE progress 엔드포인트를 permitAll에 추가
.requestMatchers(
    "/", "/index.html", ...,
    "/search", "/schedule", "/viewer",
    "/crawl-config/*/progress",  // ← 추가
).permitAll()
```

### 3.2 CrawlConfig 기본값 제거
```java
// 변경 전
@Column(name = "account_id", nullable = false)
@Builder.Default
private Long accountId = 1L;

// 변경 후
@Column(name = "account_id", nullable = false)
private Long accountId;
```

### 3.3 트랜잭션 분리
```java
// 변경 전: @Transactional로 전체 사이트를 한 트랜잭션에서 처리
@Transactional
public void executeCrawl(CrawlConfig config) { ... }

// 변경 후: @Transactional 제거, 각 사이트별 별도 트랜잭션
public void executeCrawl(CrawlConfig config) { ... }

// 별도 트랜잭션 메서드 추가
@Transactional(propagation = Propagation.REQUIRES_NEW)
public int[] saveJobPostings(...) { ... }
```

### 3.4 DB 업데이트
```sql
UPDATE crawl_config SET account_id = 4 WHERE account_id = 1;
```

## 4. 예방 방법

### 4.1 아키텍처 개선
- SSE 엔드포인트는 `permitAll`로 설정
- 각 사이트 크롤링은 별도 트랜잭션으로 처리
- 중복 에러가 전체 세션에 영향 없도록 격리

### 4.2 코드 리뷰 체크리스트
- [ ] `@Builder.Default` 사용 시 DB 기본값과 일치하는지 확인
- [ ] 새 엔드포인트 추가 시 SecurityConfig 업데이트
- [ ] `@Transactional` 범위 적절한지 확인
- [ ] `EventSource` 사용 시 쿼리 파라미터 토큰 처리 확인

## 5. 참고 자료
- [Spring Security SSE 설정](https://docs.spring.io/spring-security/reference/servlet/authentication/)
- [Hibernate 세션 관리](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#sessions)

---
*작성일: 2026-08-05*