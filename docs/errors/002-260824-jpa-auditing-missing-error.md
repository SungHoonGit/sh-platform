# 002-260824-jpa-auditing-missing-error 오류 기록

## 개요
- **발생일**: 2026-08-24
- **환경**: Java 21, Spring Boot 3.4.4, MariaDB 10.11 (resume 모듈)
- **심각도**: 🔴 Critical (지원 등록 API 전면 실패)

## 1. 오류 현상
지원 관리에서 새 지원 등록(POST `/api/v1/applications`) 시 서버 오류로 등록되지 않음.

### 1.1 에러 메시지 (예상 로그)
```
java.sql.SQLIntegrityConstraintViolationException: Column 'created_at' cannot be null
```

### 1.2 재현 단계
1. 지원 관리 탭 → [+ 새 지원 등록]
2. 회사명/공고 제목 입력 후 [등록]
3. 등록 실패

## 2. 원인 분석
### 2.1 근본 원인
`ApplicationEntity`가 Spring Data JPA 감사(auditing) 기능(`@CreatedDate`, `@LastModifiedDate`,
`@EntityListeners(AuditingEntityListener.class)`)을 사용했으나,
애플리케이션에 `@EnableJpaAuditing` 설정이 없어 리스너가 동작하지 않음.
→ insert 시 `created_at`이 null인 채로 INSERT → DB NOT NULL 제약 위반.

### 2.2 관련 코드
- 변경 전: `modules/resume/backend/src/main/java/com/shplatform/resume/infrastructure/ApplicationEntity.java`
```java
@EntityListeners(AuditingEntityListener.class)
...
@CreatedDate
@Column(name = "created_at", nullable = false, updatable = false)
private LocalDateTime createdAt;
```

## 3. 해결 방법
### 3.1 해결 과정
프로젝트의 기존 엔티티 패턴(ResumeDocumentEntity 등)은 `@PrePersist`/`@PreUpdate` 수동 타임스탬프를
사용함. 동일 패턴으로 통일.

### 3.2 최종 코드 변경
```java
// 변경 후
@Column(name = "created_at", nullable = false, updatable = false)
private LocalDateTime createdAt;

@PrePersist
void onCreate() {
    LocalDateTime now = LocalDateTime.now();
    createdAt = now;
    updatedAt = now;
}

@PreUpdate
void onUpdate() {
    updatedAt = LocalDateTime.now();
}
```

## 4. 예방 방법
- **auditing 미사용 프로젝트**에서는 `@CreatedDate`/`@LastModifiedDate`를 쓰지 않는다.
  사용하려면 반드시 `@EnableJpaAuditing` 설정 클래스 추가 여부를 먼저 확인.
- 신규 엔티티는 기존 도메인 엔티티의 타임스탬프 패턴을 복사해서 시작할 것.

## 5. 참고 자료
- [Spring Data JPA Auditing 공식 문서](https://docs.spring.io/spring-data/jpa/reference/auditing.html)

---
*작성일: 2026-08-24*
