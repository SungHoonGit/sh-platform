# 001-260812-web-push-vapid 가이드

## 개요
- **목적**: Web Push Notification과 VAPID 인증 메커니즘 이해
- **대상**: sh-platform 프로젝트 푸쉬 알림 기능 개발
- **작성일**: 2026-08-12

## 1. 개념

### 1.1 Web Push Notification이란?

웹 브라우저에서 제공하는 **시스템 레벨 알림**. 페이지를 닫아도, 브라우저가 꺼져있어도 알림을 받을 수 있는 기술.

```
[이메일 알림]     사용자가 메일함을 열어야 알림 확인 가능
[인페이지 토스트]  페이지를 열어놓아야 알림 확인 가능
[웹 푸쉬]         페이지 없어도 윈도우/맥/모바일에 알림 팝업
```

### 1.2 왜 VAPID가 필요한가?

서버가 "나 진짜 이 서비스의 서버야"라고 브라우저에 증명하는 메커니즘.

```
[VAPID 없음] -> 누구나 서버를 사칭해서 푸쉬를 보낼 수 있음
[VAPID 있음] -> 개인키로 서명 -> 브라우저가 공개키로 검증 -> 서버 인증 완료
```

### 1.3 관련 개념 관계도

```
+-----------------------------------------------------+
|                    3개 참여자                         |
|                                                     |
|  [브라우저]  <--- Service Worker로 푸쉬 수신          |
|      |                                              |
|      |  1. 구독 시 VAPID 공개키 전달                  |
|      |  2. Push Service에 구독 등록                  |
|      v                                              |
|  [Push Service]  (Google FCM / Mozilla / Apple)     |
|      |                                              |
|      |  3. 서버에서 푸쉬 요청 수신                    |
|      |  4. VAPID 서명 검증                          |
|      |  5. 브라우저에 전달                           |
|      v                                              |
|  [서버]  (sh-platform)                              |
|      |                                              |
|      |  - VAPID 개인키로 서명                        |
|      |  - 페이로드 암호화                            |
|      |  - Push Service에 POST 요청                  |
|      +----------------------------------------------+
```

## 2. 핵심 구성요소

### 2.1 용어 정리

| 용어 | 정의 | 위치 |
|------|------|------|
| **VAPID** | Voluntary Application Server Identification (RFC 8292) | 프로토콜 |
| **Service Worker** | 브라우저 백그라운드 스크립트 | 프론트엔드 |
| **Push API** | 브라우저에서 구독/수신 담당 | 브라우저 |
| **Notifications API** | 알림 표시 담당 | 브라우저 |
| **Push Service** | 서버-브라우저 중계 (FCM 등) | 브라우저 벤더 |
| **Endpoint** | Push Service가 제공하는 고유 URL | 구독 시 생성 |
| **p256dh** | 암호화 키 (브라우저가 생성) | 구독 키 |
| **auth** | 인증 키 (브라우저가 생성) | 구독 키 |

### 2.2 VAPID 키 쌍

```
[공개키]  -> 브라우저에 전달 (누구나 알 수 있음)
[개인키]  -> 서버에만 저장 (절대 외부 공개 금지)
```

- **암호화 방식**: ECDSA P-256 (타원 곡선)
- **생성 시점**: 서비스 설정 시 1회 생성
- **유효기간**: 변경하지 않는 한 영구

### 2.3 브라우저별 Push Service

| 브라우저 | Push Service | 비고 |
|----------|-------------|------|
| Chrome | Google FCM | 가장 보편적 |
| Firefox | Mozilla autopush | 독자적 |
| Edge | Google FCM | Chrome 기반 |
| Safari 16.4+ | Apple | PWA 설치 필요 |

## 3. 동작 흐름

### 3.1 구독 (Subscribe)

```
1. 사용자가 "알림 받기" 클릭 (사용자 제스처 필수!)
2. Notification.requestPermission() -> "granted" 확인
3. Service Worker 등록
4. registration.pushManager.subscribe() 호출
   |-- userVisibleOnly: true (반드시 알림 표시 약속)
   +-- applicationServerKey: VAPID 공개키
5. Push Service가 구독 객체 반환
   {
     endpoint: "https://fcm.googleapis.com/...",
     keys: {
       p256dh: "암호화키...",
       auth: "인증키..."
     }
   }
6. 서버에 구독 저장
```

### 3.2 발송 (Send)

```
1. 서버가 푸쉬 발송 준비
2. subscription.endpoint로 HTTP POST 요청
   |-- 페이로드 암호화 (subscription keys 사용)
   |-- VAPID 개인키로 JWT 서명
   +-- HTTP 헤더에 Authorization: vapid t=..., k=...
3. Push Service가 VAPID 서명 검증
4. Push Service가 브라우저에 전달
5. Service Worker의 'push' 이벤트 발생
6. registration.showNotification() 호출
7. OS 레벨 알림 표시
```

### 3.3 클릭 (Click)

```
1. 사용자가 알림 클릭
2. Service Worker의 'notificationclick' 이벤트 발생
3. clients.openWindow(url) 또는 client.focus()
4. 지정된 페이지로 이동
```

## 4. 보안 고려사항

### 4.1 암호화 2단계

| 단계 | 용도 | 사용 키 |
|------|------|---------|
| 페이로드 암호화 | Push Service가 내용을 읽지 못하도록 | subscription p256dh + auth |
| VAPID 서명 | 서버 인증 (사칭 방지) | VAPID 개인키 |

### 4.2 필수 규칙

- **HTTPS 필수** (Service Worker는 HTTPS에서만 동작)
- **VAPID 개인키 절대 프론트에 노출 금지**
- **사용자 제스처 후에만 requestPermission() 호출**
  - 페이지 로드 시 자동 호출 금지 (블라우저가 차단)
  - 한 번 거부하면 다시 요청 불가

### 4.3 구독 만료

- Chrome: 구독 만료 기간 없음 (사용자가 명시적으로 해제할 때까지)
- Safari: 30일간 상호작용 없으면 구독 만료
- 410 Gone 응답 시 DB에서 구독 삭제 필요

## 5. sh-platform 적용

### 5.1 현재 구현

```
[이메일 알림]  -> 이메일 발송 (스케줄 완료 시)
[웹 푸쉬]     -> 브라우저 알림 (스케줄 완료 시)
```

### 5.2 DB 테이블

```sql
push_subscription (
  id, account_id, endpoint, p256dh, auth_key,
  user_agent, is_active, created_at, updated_at
)
```

### 5.3 API 엔드포인트

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/v1/push/subscribe` | 구독 저장 |
| DELETE | `/api/v1/push/unsubscribe?endpoint=...` | 구독 해제 |
| GET | `/api/v1/push/vapid-public-key` | 공개키 조회 |

### 5.4 .env 설정

```
WEBPUSH_VAPID_PUBLIC_KEY=BASE64URL_공개키
WEBPUSH_VAPID_PRIVATE_KEY=BASE64URL_개인키
WEBPUSH_VAPID_SUBJECT=mailto:admin@shplatform.com
```

### 5.5 VAPID 키 생성 방법

**방법 1: Node.js (추천)**
```bash
npm install -g web-push
web-push generate-vapid-keys
```

## 6. 브라우저 호환성

| 브라우저 | 버전 | 지원 |
|----------|------|------|
| Chrome | 42+ | O |
| Firefox | 44+ | O |
| Edge | 17+ | O |
| Safari | 16.4+ | O (PWA 설치 필수) |
| iOS Safari | 16.4+ | O (홈 화면 추가 필수) |
| Internet Explorer | - | X |

## 7. 문제 해결

| 문제 | 원인 | 해결 |
|------|------|------|
| 알림 안 뜸 | Service Worker 미등록 | DevTools -> Application -> Service Workers 확인 |
| 알림 안 뜸 | Service Worker stopped | DevTools에서 start 클릭 또는 페이지 새로고침 |
| 알림 안 뜸 | Windows 알림 설정 꺼짐 | Windows 설정 -> 시스템 -> 알림 -> 켬 |
| 알림 안 뜸 | Chrome 알림 차단 | chrome://settings/content/notifications -> 허용 |
| 알림 안 뜸 | 브라우저 종료 | 웹 푸쉬는 브라우저가 켜져 있어야 함 |
| 구독 실패 | VAPID 공개키 오류 | 키 값 일치 확인 |
| Permission 거부 | 자동 호출 | 사용자 제스처(클릭) 후 호출 |
| 401 에러 | VAPID 서명 실패 | 개인키 확인 |
| Invalid point encoding | Notification 파라미터 순서 오류 | p256dh, auth 순서 확인 (아래 7.1 참조) |
| DELETE 500 에러 | @RequestBody 호환성 | @RequestParam 사용 |

### 7.1 Notification 생성자 파라미터 순서 (주의!)

`nl.martijndwars.webpush` 라이브러리의 Notification 생성자 순서:

```java
// 올바른 순서
new Notification(endpoint, userPublicKey, userAuth, payload)
//                 엔드포인트  p256dh(공개키)  auth(인증키)  페이로드

// 흔한 실수
new Notification(endpoint, userAuth, userPublicKey, payload)
//                 auth가 공개키 자리에 들어가면 "Invalid point encoding" 에러
```

**증상**: `Invalid point encoding 0x4d` - auth 키의 첫 바이트가 EC 포인트 접두사(0x04)가 아님

**원인**: auth(16바이트 랜덤)와 p256dh(65바이트 EC 공개키) 순서를 혼동

### 7.2 Service Worker 상태 확인

```
DevTools -> Application -> Service Workers
+-- activated and is running -> 정상
+-- stopped -> start 클릭 또는 페이지 새로고침
+-- waiting -> skip waiting 클릭
+-- installing -> 잠시 대기
```

### 7.3 Windows 알림 설정

웹 푸쉬가 동작하려면 OS 레벨 알림도 켜져 있어야 합니다:

```
Windows 설정 -> 시스템 -> 알림
+-- 알림 허용: 켬
+-- 집중 모드(방해 금지): 꺼짐
```

## 8. 참고 자료

- [RFC 8292 - VAPID](https://datatracker.ietf.org/doc/html/rfc8292)
- [MDN - Push API](https://developer.mozilla.org/en-US/docs/Web/API/Push_API)
- [web.dev - Push Notifications Overview](https://web.dev/articles/push-notifications-overview)
- [web-push-libs/webpush-java](https://github.com/web-push-libs/webpush-java)

---
*작성일: 2026-08-12 (최종 업데이트: 2026-08-13)*
