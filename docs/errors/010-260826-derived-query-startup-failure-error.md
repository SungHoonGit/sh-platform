# 010-260826 미사용 파생 쿼리로 auth 기동 실패 (CI 초록불·프로덕션 다운 3회째)

## 개요
- **발생일**: 2026-08-26
- **환경**: OCI Ubuntu ARM64, Java 21, Spring Boot 3.4.4 (prod profile)
- **심각도**: 🔴 Critical — auth 크래시 루프, 사이트 로그인 불가 (약 10분 내 복구)
- **패턴 분류**: 이번 주 3번째 "CI 초록불 + 프로덕션 기동 실패" (008 빈 충돌 → 009 필터체인 예외 → 010 JPA 쿼리 생성)

## 1. 오류 현상

### 1.1 에러 메시지
```
UnsatisfiedDependencyException: Error creating bean 'adminController'
  → 'loginLogService' → 'loginLogRepository':
Could not create query for
  LoginLogRepository.findByUserIdOrderByCreatedAtDesc(Long, LocalDateTime)
Reason: At least 2 parameter(s) provided but only 1 parameter(s) present in query
```

### 1.2 재현 단계
1. C-audit 구현 시 `LoginLogRepository`에 **미래용으로** 파생 쿼리 작성:
   `findByUserIdOrderByCreatedAtDesc(Long userId, LocalDateTime since)`
   - 메서드명 조건은 userId 하나뿐인데 파라미터는 2개 → 바인딩 불가
2. 컴파일 OK(시그니처 자체는 합법), Mockito 단위테스트 OK(리포지토리 미구동)
3. 배포 후 Spring Data가 **기동 중 쿼리 생성** 시도 → 실패 → 컨텍스트 폭발 → systemd 무한 재시작

## 2. 원인 분석

| 층위 | 설명 |
|------|------|
| 직접 원인 | 파생 쿼리 메서드명과 파라미터 수 불일치 (사용되지도 않을 예비 코드) |
| 검출 실패 | Spring Data의 파생 쿼리 생성은 **컨텍스트 기동 시점**에만 수행 — Mockito 슬라이스 테스트는 리포지토리 프록시를 만들지 않음 |
| 구조 요인 | auth 모듈에 `@DataJpaTest` 계열 테스트가 전무 → JPA 계층은 CI에서 한 번도 부트된 적 없음 |

**교훈**: "안 쓸 메서드를 미리 만들어두면" 안 쓰는 게 아니라 **기동 경로에 폭탄을 심는 것**이다.

## 3. 해결 방법

### 3.1 증상 제거
```java
// 삭제 (미사용 + 파라미터 불일치)
List<LoginLogEntity> findByUserIdOrderByCreatedAtDesc(Long userId, LocalDateTime since);
```

### 3.2 재발 방역 — JPA 스모크 테스트 (`7474556`)
```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.ANY)   // H2
class RepositoriesSmokeTest {
    @Autowired EntityManager entityManager;
    @Test void allRepositoryQueries_shouldBeCreatable() { assertNotNull(entityManager); }
}
```
- H2 위에 JPA 슬라이스만 부트 → **모든 리포지토리 파생쿼리 생성 검증**
- 역방향 검증 완료: 고의로 깨진 메서드 삽입 시 이 테스트가 FAILED 확인 후 제거

## 4. 예방 방법

1. **YAGNI**: 사용하지 않을 리포지토리 메서드를 미리 만들지 않는다
2. **JPA 스모크 상시화**: `RepositoriesSmokeTest`가 auth 모듈 CI에서 항상 실행됨
3. **동일 유형 총정리**: 오류기록 008(빈 충돌)/009(필터체인)/010(JPA 쿼리) 모두
   "Mockito 단위테스트 한계" 공유 — 향후 신규 계층 추가 시마다 대응 스모크 도입 검토
   - Security Filter 체인 변경 시 → 부팅 스모크(lazy-init 또는 @SpringBootTest)

## 5. 참고 자료
- 관련: `docs/errors/008`(AdminController 빈 충돌), `docs/errors/009`(OAuth2 세션 500)
- Spring Data JPA — Query Creation & Validation
- 관련 커밋: `5488f2f`(원인 도입), `7474556`(수정+방역)

---
*작성일: 2026-08-26*
