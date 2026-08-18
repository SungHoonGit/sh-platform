# 006-260818-service-restart-loop-incident-report.md

## 개요
- **작성일**: 2026-08-18
- **작성자**: AI Assistant / 사용자
- **장애 유형**: 🔴 Critical — 서비스 무한 재시작 루프 → CPU 100% 점유 + 배포 지연
- **영향 범위**: 
  - 서버 CPU 항시 100% (수 시간 지속)
  - resume/portfolio 서비스 기동 불가
  - GitHub Actions 배포 지연
  - OAuth2 소셜 로그인 작업 중 발견 (8/18 13:12경)

## 1. 장애 요약

| 항목 | 내용 |
|------|------|
| **증상** | `ps`에서 java PID가 계속 바뀌며 CPU 총합 ~100% 유지 |
| **발견** | 소셜 로그인 설정 중 서버 모니터링하다 발견 |
| **원인** | `common`의 `NotificationService`가 `JavaMailSender`를 필수 주입 → resume/portfolio(메일 설정 없음) 기동 실패 → `Restart=always` 무한 재시작 |
| **해결** | `JavaMailSender` 선택 주입(`required=false`) 변경 → 모든 서비스 정상 기동 |
| **영향 시간** | 약 8/18 13:12 ~ 13:40 (워밍업 포함 정상화까지) |

## 2. 장애 타임라인

| 시간 | 이벤트 |
|------|--------|
| 13:12 | 배포로 4개 서비스 재시작. resume/portfolio 기동 실패 시작 |
| 13:22 | resume `UnsatisfiedDependencyException` (JavaMailSender 없음) |
| 13:23~13:25 | resume 1분 간격으로 재시작 반복 (PID 2560525→2560948→2561380→2561754) |
| 13:34~13:37 | 수동 `daemon-reload` + `restart` → Xmx 적용 확인 (auth 768m, scraper 1024m) |
| 13:38~ | 배포 완료 + 워밍업 종료 → CPU 정상화, 4개 서비스 정상 기동 |

## 3. 근본 원인 분석

### 3.1 원인
```
공통 모듈 (common) NotificationService
  └─ @RequiredArgsConstructor → JavaMailSender 필수 주입
       └─ resume/portfolio: spring.mail 설정 없음 → JavaMailSender 빈 없음
            └─ UnsatisfiedDependencyException → 기동 실패
                 └─ systemd Restart=always → 즉시 재시작 → 실패 반복
                      └─ 재시작마다 JVM 워밍업 → CPU 100% 지속
```

### 3.2 왜 다른 서비스는 정상이었나
- **scraper**: `application.yml`에 `spring.mail` 설정 있음 → `JavaMailSender` 빈 생성됨 ✅
- **auth**: 자체 메일 설정(`application-prod.yml`) 있음 ✅
- **resume/portfolio**: `spring.mail` 설정 없음 → 빈 미생성 ❌

### 3.3 배포 지연의 원인
- 재시작 루프가 서비스 리스너/healthcheck를 방해
- 빌드 후 배포 스크립트가 서비스 재시작 대기 중 타임아웃 반복

## 4. 해결 조치

### 4.1 코드 수정 (`ea77d4c`)
```java
// 변경 전
@RequiredArgsConstructor
public class NotificationService {
    private final JavaMailSender mailSender;  // 필수 주입
}

// 변경 후
public class NotificationService {
    private final JavaMailSender mailSender;

    @Autowired
    public NotificationService(NotificationConfigRepository configRepository,
                               NotificationLogRepository logRepository,
                               @Autowired(required = false) JavaMailSender mailSender) {
        ...
        this.mailSender = mailSender;
    }

    public void sendEmail(String to, String content) {
        if (mailSender == null) {
            log.warn("[EMAIL] JavaMailSender not configured. Email skipped");
            return;
        }
        ...
    }
}
```

### 4.2 서버 설정 (`수동`)
- `daemon-reload` + `restart` 후 Xmx 반영 확인
- auth/resume/portfolio: `-Xmx768m -Xms256m`
- scraper: `-Xmx1024m -Xms256m` (채용공고 수집 대용량 처리 대비)

## 5. 교훈 및 개선점

### 5.1 재발 방지
- [x] 공통 모듈 의존성은 **선택 주입 또는 `@ConditionalOnProperty`** 사용
- [x] `Restart=always` 사용 시 기동 실패 원인을 로그로 사전 검증
- [ ] 신규 모듈 배포 전 **테스트 환경에서 부팅 테스트** 필수

### 5.2 모니터링 개선 (다음 작업)
- [ ] Grafana subpath 로딩 문제 수정 (`/grafana/` 화면 안 뜸)
- [ ] **Grafana Alerting** 설정 — CPU > 90% 지속 시 이메일 알림
  - `MAIL_USERNAME`/`MAIL_PASSWORD` (.env) 활용해 SMTP 알림 채널 구성
- [ ] 서비스 기동 실패 시 알림 — `systemd` `OnFailure` → 알림 훅
- [ ] 재시작 루프 감지 대시보드 (restart 횟수 카운트)

### 5.3 배포 프로세스 개선
- [ ] 배포 워크플로우에 **서비스 기동 확인(healthcheck) + 타임아웃** 강화
- [ ] 배포 시 4개 서비스 **순차 재시작** (동시 워밍업으로 CPU 100% 방지)

## 6. 참고 자료
- [Spring Boot 메일 자동 설정 조건](https://docs.spring.io/spring-boot/reference/features/email.html)
- [Spring @Autowired(required=false)](https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired.html)
- [systemd Restart=always](https://www.freedesktop.org/software/systemd/man/latest/systemd.service.html)

---
*작성일: 2026-08-18*