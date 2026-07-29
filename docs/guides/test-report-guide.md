# 테스트 리포트 & 문서 자동화 가이드

## 1. JUnit 테스트 리포트

### 1.1 개념

JUnit으로 테스트 코드를 작성하면 Gradle이 실행 결과를 HTML 파일로 자동 생성한다.

```
./gradlew test
  ↓
build/reports/tests/test/index.html   ← 브라우저로 열 수 있는 HTML
build/test-results/test/*.xml          ← CI가 읽는 기계용 데이터
```

### 1.2 HTML 리포트 보는 법

```
로컬: sh-platform-auth/build/reports/tests/test/index.html
         ↓ 브라우저에 더블클릭
  "Test Summary" 대시보드 (tests, failures 수)
         ↓ 패키지 클릭
  각 테스트 메서드별 성공/실패/시간
```

### 1.3 CI (GitHub Actions) 에서 보는 법

`.github/workflows/deploy-backend.yml`에 추가한 코드:

```yaml
- name: Upload Test Report
  uses: actions/upload-artifact@v4
  if: success() || failure()
  with:
    name: test-report
    path: '**/build/reports/tests/test/'
```

이 한 블록이 `build/reports/tests/test/` 폴더를 통째로 zip으로 묶어서 Actions 결과에 첨부한다.

**확인 경로:**
```
GitHub 저장소 → Actions → 해당 워크플로우 클릭
  → 하단 "Artifacts" 섹션 → test-report.zip 다운로드
  → 압축 풀고 index.html 열기
```

### 1.4 왜 유용한가

| 상황 | 효과 |
|------|------|
| PR 올리기 전 | 내가 추가한 기능이 기존 기능을 안 깨뜨리는지 확인 |
| CI에서 실패 | "XX 테스트 실패" 메시지로 원인 즉시 파악 |
| 코드 리뷰 | 리뷰어가 "테스트 커버리지 충분한가?" 확인 가능 |
| 배포 전 자동 검증 | 테스트 하나라도 실패하면 배포 중단 (안전장치) |

---

## 2. Javadoc

### 2.1 개념

Java 소스 코드에 `/** ... */` 주석을 달면 Gradle이 HTML 문서를 자동 생성한다.

```java
/**
 * 회원 정보를 조회한다.
 *
 * @param userId 사용자 ID (PK)
 * @return User 객체 (email, name, role 포함)
 * @throws BusinessException NOT_FOUND - 사용자가 존재하지 않는 경우
 */
public User getUser(Long userId);
```

### 2.2 생성 명령어

```bash
./gradlew javadoc
# → build/docs/javadoc/index.html
```

### 2.3 적용 예시 (API 문서)

생성된 HTML은 Spring RestDocs나 Swagger처럼 예쁘진 않지만,
**메서드 목록, 파라미터, 반환값, 예외**를 정리해서 보여준다.

### 2.4 CI에 적용하려면

```yaml
- name: Generate Javadoc
  run: ./gradlew javadoc

- name: Upload Javadoc
  uses: actions/upload-artifact@v4
  with:
    name: javadoc
    path: '**/build/docs/javadoc/'
```

---

## 3. 전체 플로우

```
개발자가 코드 작성
  → Javadoc 주석 작성 (인터페이스 메서드)
  → JUnit 테스트 코드 작성
  → git commit + push
  → GitHub Actions 실행
      1. ./gradlew build (테스트 포함)
      2. 테스트 결과 HTML 생성
      3. HTML을 artifact로 업로드
      4. (실패 시) build 실패, deploy 중단
      5. (성공 시) SSH로 서버 배포
```

---

## 4. 핵심 요약

| 도구 | 입력 | 출력 | 확인 방법 |
|------|------|------|-----------|
| JUnit | `src/test/` 테스트 코드 | `build/reports/tests/test/index.html` | 브라우저 or Actions Artifact |
| Javadoc | `/** */` 주석 | `build/docs/javadoc/index.html` | 브라우저 or Actions Artifact |
| Gradle | `build.gradle.kts` | 위 두 개를 자동 생성 | `./gradlew test`, `./gradlew javadoc` |

별도 플러그인 설치 없이 **Spring Boot Gradle 기본 내장 기능**만으로 동작한다.
