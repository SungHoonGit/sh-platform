# 008-260825 AdminController 빈 이름 충돌로 auth 기동 실패 (크래시 루프)

## 개요
- **발생일**: 2026-08-25
- **환경**: OCI Ubuntu ARM64, Java 21, Spring Boot 3.4.4 (prod profile)
- **심각도**: 🔴 Critical — auth 서비스 약 4시간 30분 다운 (전체 사이트 로그인 불가)

## 1. 오류 현상

### 1.1 에러 메시지
```
org.springframework.beans.factory.BeanDefinitionStoreException:
Failed to parse configuration class [com.shplatform.ShPlatformApplication]

Caused by: org.springframework.context.annotation.ConflictingBeanDefinitionException:
Annotation-specified bean name 'adminController' for bean class
[com.shplatform.auth.api.admin.AdminController] conflicts with existing,
non-compatible bean definition of same name and class
[com.shplatform.auth.api.AdminController]
```

### 1.2 재현 단계
1. Redis Phase 7(관리자 API)에서 `api/admin/AdminController.java` 신규 생성 (기존 `api/AdminController.java` 존재)
2. master push → GitHub Actions 배포 (빌드·단위테스트 전부 통과, ✅ 초록불)
3. 서버에서 auth 재기동 → 즉시 크래시 → systemd `Restart=always`에 의해 15초마다 무한 재시작 (restart counter 1052+)

### 1.3 증상 특징 (진단을 어렵게 한 점)
- **CI는 초록불**: auth 테스트가 전부 `@ExtendWith(MockitoExtension.class)` 단위 테스트라
  스프링 컨텍스트를 띄워보지 않음 → 컴포넌트 스캔 충돌은 런타임에만 발생
- `curl /api/health` 빈 응답 (포트 미바인드)
- scraper/resume는 정상 동작 → 겉보기엔 "일부만 이상함"

## 2. 원인 분석

### 2.1 근본 원인
스프링 컴포넌트 스캔은 **클래스 단순명**(FQCN 아님)을 기본 빈 이름으로 사용한다.
같은 애플리케이션 컨텍스트 안에 단순명이 같은 `@RestController`가 2개 있으면:

| 클래스 | 빈 이름 | 역할 |
|--------|---------|------|
| `com.shplatform.auth.api.AdminController` | `adminController` | 세션 관리/애널리틱스 (Redis Phase 6) |
| `com.shplatform.auth.api.admin.AdminController` | `adminController` ← **충돌** | 사용자/테넌트 관리 (Redis Phase 7) |

패키지가 달라도 단순명만 같으면 충돌한다.

### 2.2 왜 테스트/CI를 통과했나
- `ConfigurationClassPostProcessor`의 컴포넌트 스캔은 애플리케이션 기동 최초 단계에서 실행
- Mockito 단위 테스트는 스프링 컨텍스트 없이 클래스만 조립 → 스캔 단계 자체를 검증하지 못함
- 결과: **빌드 ✅ + 단위테스트 ✅ + 프로덕션 기동 ❌** 조합이 가능

### 2.3 과거 동일 사례 (재발)
- 2026-08-24: common 모듈 `FileController` ↔ resume `FileController` 충돌
  → `ResumeFileController`로 리네임해서 해결 (docs/daily/2026-08-24-work-log.md)
- **동일 유형 2회째** → 패턴화 필요 (아래 예방 참조)

## 3. 해결 방법

### 3.1 해결 과정
1. 서버 로그로 루트 cause 확정: `journalctl -u sh-platform-auth | grep -A50 "Application run failed"`
2. auth + common 모듈의 컴포넌트 클래스(`@RestController/@Service/@Repository/@Component/@Configuration`) 대상으로 단순명 중복 일괄 스캔 → 충돌은 AdminController 1건뿐임을 확인
3. 신규 쪽 리네임: `git mv api/admin/AdminController.java api/admin/AdminApiController.java`
4. 로컬 부팅 스모크로 검증 (아래 3.3)

### 3.2 최종 코드 변경
```bash
# 변경 전
modules/auth/backend/src/main/java/com/shplatform/auth/api/admin/AdminController.java  (class AdminController)

# 변경 후
modules/auth/backend/src/main/java/com/shplatform/auth/api/admin/AdminApiController.java  (class AdminApiController)
```
- URL 매핑은 양쪽 모두 `/api/v1/admin` 베이스지만 서브경로가 안 겹침
  (`/stats`,`/users`,`/tenants` vs `/analytics`,`/sessions`,`/login-logs`) → 공존 가능
- 커밋: `a2c9f3e fix: 관리자 AdminController 빈 이름 충돌로 auth 기동 실패 수정`

### 3.3 검증 방법 (DB 없이 컨테이너 기동 확인)
빈 이름 충돌은 DB 연결보다 **먼저**(컴포넌트 스캔) 발생하므로, lazy-init으로 스캔만 검증 가능:
```bash
java -jar modules/auth/backend/build/libs/sh-platform-auth-*.jar \
  --spring.main.lazy-initialization=true --server.port=18080
```
- 수정 전: `ConflictingBeanDefinitionException` 즉발 (prod와 동일 지점)
- 수정 후: 스캔 통과 → `entityManagerFactory`(DB) 단계에서만 실패 = 정상

## 4. 예방 방법

1. **네이밍 규칙 (AGENTS.md 명시)**: 한 모듈(+common) 안에서는 패키지가 달라도
   컴포넌트 클래스 단순명 중복 금지. 컨트롤러는 `{Domain}{역할}Controller`로 유니크하게.
   - 예: 관리자 API 분리 시 `AdminApiController`, `AdminSessionApiController` 등
2. **CI 보강 (권장)**: 컨텍스트 기동 스모크 추가
   - 옵션 A: 위 3.3 lazy-init 부팅 스크립트를 deploy 워크플로우 build 후 실행
   - 옵션 B: H2 기반 `@SpringBootTest contextLoads()` 1건 추가 (auth)
3. **배포 직후 헬스체크**: deploy 스크립트 마지막에
   `curl -sf http://localhost:8080/api/health || exit 1` 추가 시 크래시 루프 배포를 조기 차단
4. **크래시 루프 알림**: restart counter 급증 시 Grafana alert (기존 005/006 사례와 동일 패턴)

## 5. 참고 자료
- Spring 공식 — Customizing Bean Naming (`AnnotationBeanNameGenerator`는 단순명 사용)
- 관련 문서: `docs/errors/005-260818-resume-portfolio-restart-loop-cpu-error.md` (크래시 루프 유형)
- 관련 일지: `docs/daily/2026-08-25-work-log.md` (Redis 도입 당일 발생)

---
*작성일: 2026-08-25*
