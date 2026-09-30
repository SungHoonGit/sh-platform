# 017-260930-resume-service-arg-order-reversed-error 오류 기록

## 개요
- **발생일**: 2026-09-30 (사용자 보고: 프로젝트 관리 등 수정·삭제 오류)
- **환경**: resume 백엔드 (Spring Boot 3.4.4, Java 21)
- **심각도**: 🔴 Critical — resume 6개 도메인(프로젝트/자기소개/경력/학력/기술/자격증/포트폴리오)의 **수정·삭제 전부가 NOT_FOUND/FORBIDDEN 실패**

## 1. 오류 현상
### 1.1 에러 메시지
- 프론트: "저장에 실패했습니다" (API_ERROR_404/403)
- 백엔드: `BusinessException NOT_FOUND` 또는 `FORBIDDEN`

### 1.2 재현 단계
1. resume 앱에서 항목(예: 프로젝트 관리) 수정 → 저장
2. 항상 실패 (등록/조회/순서변경은 정상 — update/delete만 실패)

## 2. 원인 분석
### 2.1 근본 원인
**인터페이스와 구현의 파라미터 순서가 달라도 Java는 컴파일이 통과한다**
(타입 시그니처가 `(Long, Long, Long, Request)`로 동일하기 때문).

```java
// 인터페이스 (PortfolioItemService)
PortfolioItemResponse updatePortfolioItem(Long userId, Long itemId, Long documentId, Request request);
// 구현 (PortfolioItemServiceImpl) — itemId와 documentId가 역전!
public PortfolioItemResponse updatePortfolioItem(Long userId, Long documentId, Long itemId, Request request)
```
컨트롤러는 **인터페이스 순서**로 호출: `update(accountId, id, documentId, request)` →
구현으로는 `documentId = id(작업물ID)`, `itemId = documentId(문서ID)`로 **역전 바인딩** →
`findById(문서ID)` → 포트폴리오 항목이 없어 항상 NOT_FOUND (혹은 우연히 같은 ID면 FORBIDDEN).

### 2.2 영향 범위 (전수 스캔 결과 — 12시그니처)
| 도메인 | update | delete |
|--------|--------|--------|
| PortfolioItem | ✗ | ✗ |
| Introduction | ✗ | ✗ |
| Project | ✗ | ✗ |
| Education | ✗ | ✗ |
| Skill | ✗ | ✗ |
| Certificate | ✗ | ✗ |
create/get/reorder는 순서가 일치해 정상 → "수정만 안 된다"로 발견됨.

### 2.3 왜 테스트가 놓쳤나
테스트가 `@InjectMocks XServiceImpl xService` **구현 타입으로 직접 호출**해 구현의 순서 기준으로
작성돼 있었음 — 컨트롤러(인터페이스 기준)와 서로 다른 순서를 검증한 것.

## 3. 해결 방법
### 3.1 해결 과정
- 구현 6파일 12시그니처를 **인터페이스 순서로 교정** (본문은 변수명 기라 영향 없음)
- 회귀 방지: 6테스트를 **인터페이스 타입 필드**로 전환 → 구현 시그니처를 다시 틀리면 컴파일/런타임에서 즉시 실패.
  Mockito는 인터페이스에 `@InjectMocks`를 직접 주입할 수 없으므로
  `@InjectMocks XServiceImpl impl` + `private XService service` + `@BeforeEach` 연결로 해결.
- 검증: `:modules:resume:backend:test` 6클래스 통과 + 74개 메서드 인자 순서 전수 스캔 mismatch 0

### 3.2 최종 코드 변경
```java
// 변경 전 (구현)
public PortfolioItemResponse updatePortfolioItem(Long userId, Long documentId, Long itemId, ...)
// 변경 후 (인터페이스와 동일)
public PortfolioItemResponse updatePortfolioItem(Long userId, Long itemId, Long documentId, ...)
```

## 4. 예방 방법
- **서비스 인터페이스가 public API인 곳** — 구현 시그니처의 연속 같은 타입(Long, Long) 인자는 인터페이스 순서를 반드시 지킬 것
- 테스트는 **반드시 인터페이스 타입으로 호출**할 것 (구현 타입 호출 = 순서 검증 포기)
- 스캔 방법: 인터페이스/구현의 `Long \w+, Long \w+` 연속 인자명 대조 (이번 세션 스크립트 재사용 가능)

## 5. 참고 자료
- 커밋 `866c30a` (fix 1번)
- 관련: `docs/errors/018-260930-shell-source-missing-error.md` (같은 배포)

---
*작성일: 2026-09-30*
