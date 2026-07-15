# Javadoc 가이드

## 개념

Java 소스 코드의 `/** ... */` 주석을 HTML 문서로 자동 변환. Spring RestDocs나 Swagger처럼 예쁘진 않지만, **백엔드 개발자가 코드 수준에서 참고할 문서**를 만들어준다.

## 생성 명령어

```bash
./gradlew javadoc
# → build/docs/javadoc/index.html
```

모듈별:

```bash
./gradlew :sh-platform-auth:javadoc    # auth 모듈만
```

## 웹에서 보기

```
https://sunghoonyk.duckdns.org/javadoc/
```

매 deploy 시 `./gradlew :sh-platform-auth:javadoc` 이 실행되어 자동 갱신된다.

## 작성 규칙

### 대상

| 대상 | 적용 | 이유 |
|------|:----:|------|
| interface 메서드 (public) | 필수 | API 문서 기준 |
| ErrorCode enum | 필수 | 에러 의미 전달 |
| DTO record | 권장 | 필드 설명 |
| private 메서드 | 생략 | 내부 구현 |

### 포맷

```java
/**
 * (명령형으로) 회원가입을 처리한다.
 *
 * @param request  이메일, 비밀번호, 이름
 * @return 생성된 사용자 정보 (id, email, name, role)
 * @throws BusinessException EMAIL_NOT_VERIFIED - 이메일 인증되지 않음
 *                           DUPLICATE_EMAIL    - 중복 이메일
 */
User signup(SignupRequest request);
```

- 첫 문장은 동사로 끝맺음 (`-한다`, `-조회한다`)
- `@param`, `@return`, `@throws` 순서
- `@Override` 메서드는 생략 가능 (부모 문서 상속)

## Javadoc vs Swagger

| 항목 | Javadoc | Swagger |
|------|---------|---------|
| 대상 독자 | 백엔드 개발자 | 프론트/QA/외부 |
| 정보 | 클래스, 메서드, 파라미터 | API 엔드포인트, 요청/응답 JSON |
| 기능 | 설명만 | "Try it out" 으로 실제 호출 |
| IDE 연동 | IDE에서 주석으로 바로 보임 | 브라우저 필요 |

## nginx 설정

```nginx
location /javadoc/ {
    alias /home/ubuntu/sh-platform/sh-platform-auth/build/docs/javadoc/;
    autoindex on;
}
```
