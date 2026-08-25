# 001-260813-web-push-notification-error

## 개요
- **발생일**: 2026-08-13
- **환경**: Windows, Java 21, Spring Boot 3.4.4, web-push 5.1.2
- **심각도**: Critical (푸쉬 알림 전체 불능)

## 1. 오류 현상

### 1.1 에러 메시지
```
[PUSH] Failed to send to https://fcm.googleapis.com/fcm/send/...: Invalid point encoding 0x4d
[PUSH] Failed to send to https://fcm.googleapis.com/fcm/send/...: Invalid point encoding 0x6f
[PUSH] Failed to send to https://fcm.googleapis.com/fcm/send/...: Invalid point encoding 0x64
```
- 모든 구독(17건)에서 동일하게 실패
- 각 구독마다 첫 바이트가 다름 (0x4d, 0x6f, 0x64, 0x73 등)

### 1.2 재현 단계
1. 크롤 스케줄 편집에서 푸쉬 알림 체크
2. 크롤 실행
3. 서버 로그에서 "Invalid point encoding" 에러 확인

## 2. 원인 분석

### 2.1 근본 원인
`nl.martijndwars.webpush` 라이브러리의 `Notification` 생성자 파라미터 순서 오류

**라이브러리 정의 (올바름)**:
```java
public Notification(String endpoint, String userPublicKey, String userAuth, String payload)
//                            엔드포인트   p256dh(공개키)    auth(인증키)    페이로드
```

**우리 코드 (오류)**:
```java
Notification notification = new Notification(
    sub.getEndpoint(),
    sub.getAuthKey(),    // auth(16바이트) - 공개키 자리에 배치
    sub.getP256dh(),     // p256dh(65바이트) - 인증키 자리에 배치
    payload);
```

### 2.2 왜 "Invalid point encoding"인가?
- `userPublicKey` 파라미터는 EC 공개키(65바이트, 0x04 접두사)여야 함
- auth 키(16바이트 랜덤 데이터)가 공개키 자리에 들어감
- BouncyCastle이 auth 바이트를 EC 포인트로 파싱 시도
- auth의 첫 바이트(예: 0x4d='M')가 유효한 EC 포인트 접두사(0x04)가 아님
- -> `Invalid point encoding 0x4d`

### 2.3 관련 코드
- 파일: `common/src/main/java/com/shplatform/common/notification/WebPushService.java:72-76`
- 코드:
```java
// 변경 전 (오류)
Notification notification = new Notification(
    sub.getEndpoint(),
    sub.getAuthKey(),    // auth가 공개키 자리에
    sub.getP256dh(),     // p256dh가 인증키 자리에
    payload);

// 변경 후 (올바름)
Notification notification = new Notification(
    sub.getEndpoint(),
    sub.getP256dh(),     // p256dh(공개키)
    sub.getAuthKey(),    // auth(인증키)
    payload);
```

## 3. 해결 방법

### 3.1 해결 과정
1. 에러 메시지에서 "Invalid point encoding" 식별
2. web-push 라이브러리 Notification 클래스 소스 코드 확인
3. 생성자 파라미터 순서 확인: `(endpoint, userPublicKey, userAuth, payload)`
4. 현재 코드와 비교하여 auth/p256dh 순서 역전 발견
5. 순서 교체 후 테스트

### 3.2 최종 코드 변경
```java
// 변경 후
Notification notification = new Notification(
    sub.getEndpoint(),
    sub.getP256dh(),
    sub.getAuthKey(),
    payload);
```

## 4. 예방 방법
- 라이브러리 업그레이드 시 생성자/메서드 시그니처 변경 확인
- 암호화 관련 코드는 단위 테스트에서 다양한 키로 검증
- "Invalid point encoding" 에러 발생 시 파라미터 순서 의심

## 5. 참고 자료
- [web-push-libs/webpush-java Notification.java](https://github.com/web-push-libs/webpush-java/blob/master/src/main/java/nl/martijndwars/webpush/Notification.java)

---
*작성일: 2026-08-13*
