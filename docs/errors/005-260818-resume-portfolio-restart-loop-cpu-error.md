# 005-260818-resume-portfolio-restart-loop-cpu-error.md

## 개요
- **발생일**: 2026-08-18
- **환경**: Ubuntu 24.04 (ARM64), Java 21, Spring Boot 3.4.4, systemd
- **심각도**: 🔴 Critical (CPU 100% 상시 점유)

## 1. 오류 현상

### 1.1 증상
- 서버 CPU가 **항시 100%**로 유지됨
- `ps -ef | grep java` 결과 프로세스 PID가 계속 변경됨 (재시작 반복)

### 1.2 관련 로그
```
Aug 18 13:22:57 java[2560525]: UnsatisfiedDependencyException:
  Error creating bean with name 'notificationService':
  No qualifying bean of type 'org.springframework.mail.javamail.JavaMailSender' available
Aug 18 13:22:57 systemd[1]: sh-platform-resume.service: Main process exited, code=exited, status=1/FAILURE
Aug 18 13:23:48 systemd[1]: sh-platform-resume.service: Failed with result 'exit-code'  ← 1분 후 다시 시작
Aug 18 13:24:37 systemd[1]: sh-platform-resume.service: Main process exited, code=exited
Aug 18 13:25:29 systemd[1]: sh-platform-resume.service: Main process exited, code=exited
```

### 1.3 재현 단계
1. resume/portfolio 모듈 배포 (개발 초기 단계, `spring.mail` 설정 없음)
2. `common` 모듈의 `NotificationService`가 `JavaMailSender`를 필수 주입
3. `JavaMailSender` 빈 미생성 → `UnsatisfiedDependencyException` → 기동 실패
4. systemd `Restart=always`가 즉시 재시작 → 실패 반복

## 2. 원인 분석

### 2.1 근본 원인
- `NotificationService`에 `@RequiredArgsConstructor` (Lombok)로 `JavaMailSender` 필수 주입
- resume/portfolio는 `application.yml`에 `spring.mail` 설정이 **없음** → `JavaMailSender` 빈 자동 생성 안 됨
- scraper는 `spring.mail` 설정이 있어 정상 기동
- resume/portfolio만 기동 실패 → `Restart=always` 무한 재시작 루프

### 2.2 왜 CPU 100%였나
- 재시작마다 JVM 부팅 + Spring 컨텍스트 초기화 **워밍업** 발생 (순간 CPU ~50%)
- resume/portfolio가 **1분 간격으로 번갈아 재시작** → CPU가 항상 100%에 가까움

### 2.3 관련 코드
- 파일: `common/src/main/java/com/shplatform/common/notification/NotificationService.java`
- 원인: `@RequiredArgsConstructor`로 `private final JavaMailSender mailSender;` 강제 주입

## 3. 해결 방법

### 3.1 해결 과정
- `JavaMailSender`를 **선택 주입(optional)** 으로 변경
- 메일 미설정 모듈은 `mailSender = null`로 주입되고, 이메일 전송 시 **로그만 남기고 스킵**
- scraper(메일 설정 있음)는 기존대로 동작

### 3.2 최종 코드 변경
```java
// 변경 전
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationConfigRepository configRepository;
    private final NotificationLogRepository logRepository;
    private final JavaMailSender mailSender;
}

// 변경 후
public class NotificationService {
    private final JavaMailSender mailSender;

    @Autowired
    public NotificationService(NotificationConfigRepository configRepository,
                               NotificationLogRepository logRepository,
                               @Autowired(required = false) JavaMailSender mailSender) {
        this.configRepository = configRepository;
        this.logRepository = logRepository;
        this.mailSender = mailSender;
    }

    public void sendEmail(String to, String content) {
        if (mailSender == null) {
            log.warn("[EMAIL] JavaMailSender not configured. Email skipped: to={}", to);
            return;
        }
        ...
    }
}
```

## 4. 예방 방법
- **공통 모듈의 필수 의존성은 모듈마다 다를 수 있으므로**, 선택 주입(`required = false`) 또는 `@ConditionalOnProperty`로 구성
- **개발 초기 단계 모듈**은 배포 시 systemd `Restart=always` 루프를 대비해 로그 사전 확인
- 재시작 루프 의심 시: `journalctl -u {서비스} --since "10 min ago"`로 실패 원인 확인
- 단일 코어 서버에서는 **동시 재시작 워밍업**으로 CPU 100%가 발생할 수 있으므로, 재시작 시 순차적으로

## 5. 참고 자료
- [Spring Boot: JavaMailSender 자동 설정 조건](https://docs.spring.io/spring-boot/reference/features/email.html)
- [Spring: @Autowired(required = false) 선택 주입](https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired.html)

---
*작성일: 2026-08-18*