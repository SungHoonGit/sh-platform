# SSE와 WebSocket 비교 및 nginx 설정 가이드

## 1. 개요

이 문서는 실시간 통신 프로토콜인 SSE와 WebSocket의 차이점, 그리고 nginx에서의 설정 방법을 다룹니다.

---

## 2. SSE vs WebSocket 비교

### 2.1 기본 개념

| 항목 | SSE (Server-Sent Events) | WebSocket |
|------|--------------------------|-----------|
| **통신 방향** | 단방향 (서버 → 클라이언트) | 양방향 (서버 ↔ 클라이언트) |
| **프로토콜** | HTTP 기반 | 별도 프로토콜 (ws://, wss://) |
| **브라우저 지원** | IE 제외 모든 모던 브라우저 | 모든 브라우저 |
| **자동 재연결** | ✅ 내장 (EventSource) | ❌ 직접 구현 필요 |
| **데이터 형식** | 텍스트 (기본), JSON | 텍스트 + 바이너리 |
| **연결 수** | HTTP/1.1: 6개 제한, HTTP/2: 무제한 | 무제한 |
| **복잡도** | 쉬움 | 복잡함 |
| **방화벽 친화** | ✅ (HTTP 기반) | ❌ (일부 환경에서 차단) |

### 2.2 상세 비교

#### SSE (Server-Sent Events)

```
[클라이언트]                    [서버]
    |                            |
    |--- GET /events ----------->|
    |                            |
    |<-- data: {"id":1} --------|  (이벤트 1)
    |<-- data: {"id":2} --------|  (이벤트 2)
    |<-- data: {"id":3} --------|  (이벤트 3)
    |                            |
    | (연결 유지, 새 이벤트 대기)  |
```

**장점:**
- 구현이 쉬움
- 자동 재연결 기능 내장
- HTTP/2와 호환
- 기존 인프라 재사용 가능

**단점:**
- 클라이언트에서 서버로 데이터 전송 불가
- HTTP/1.1에서는 동시 연결 수 제한 (6개)

#### WebSocket

```
[클라이언트]                    [서버]
    |                            |
    |--- HTTP Upgrade ---------->|
    |<-- 101 Switching ----------|
    |                            |
    |<-- {"type":"msg"} -------->|
    |<-- {"type":"msg"} <--------|
    |                            |
    | (양방향 실시간 통신)         |
```

**장점:**
- 양방향 실시간 통신
- 텍스트 + 바이너리 데이터 지원
- 낮은 레이턴시
- 연결 수 제한 없음

**단점:**
- 구현 복잡도 높음
- 자동 재연결 구현 필요
- 일부 방화벽에서 차단 가능

---

## 3. 사용 사례

### 3.1 SSE가 적합한 경우

| 사례 | 설명 |
|------|------|
| 실시간 알림 | 서버에서 클라이언트로 알림 전송 |
| 뉴스 피드 | 새로운 뉴스 실시간 업데이트 |
| 진행 상태 표시 | 파일 업로드/다운로드 진행률 |
| **채용공고 수집 알림** | **크롤링 진행 상황 실시간 표시** ✅ |
| 대시보드 데이터 | 실시간 통계 업데이트 |

### 3.2 WebSocket이 적합한 경우

| 사례 | 설명 |
|------|------|
| 채팅 | 양방향 메시지 교환 |
| 온라인 게임 | 실시간 게임 상태 동기화 |
| 협업 에디터 | 동시 편집 |
| 화면 공유 | 실시간 비디오 스트리밍 |
| IoT 제어 | 기기 제어 명령 전송 |

---

## 4. 기술 구현 비교

### 4.1 구현 복잡도

| 항목 | SSE | WebSocket |
|------|-----|-----------|
| 서버 구현 | 5줄 | 20줄+ |
| 클라이언트 | 10줄 | 30줄+ |
| 재연결 로직 | 자동 | 직접 구현 |
| 에러 핸들링 | 간단 | 복잡 |

### 4.2 예시 코드

#### SSE 서버 (Spring Boot)

```java
@GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter getEvents() {
    SseEmitter emitter = new SseEmitter(600_000L); // 10분
    
    // 비동기로 이벤트 전송
    CompletableFuture.runAsync(() -> {
        try {
            emitter.send(SseEmitter.event()
                .name("message")
                .data(Map.of("id", 1, "text", "안녕하세요")));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    });
    
    return emitter;
}
```

#### SSE 클라이언트 (JavaScript)

```javascript
const es = new EventSource('/events');

es.addEventListener('message', (e) => {
    const data = JSON.parse(e.data);
    console.log('받은 데이터:', data);
});

es.onerror = (e) => {
    console.log('연결 끊김, 재연결 중...');
    // EventSource는 자동으로 재연결 시도
};
```

#### WebSocket 서버 (Spring Boot)

```java
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 모든 클라이언트에게 브로드캐스트
        sessions.values().forEach(s -> {
            try {
                s.sendMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
```

#### WebSocket 클라이언트 (JavaScript)

```javascript
const ws = new WebSocket('ws://localhost:8080/chat');

ws.onopen = () => {
    console.log('연결됨');
    ws.send(JSON.stringify({ type: 'join', room: 'general' }));
};

ws.onmessage = (e) => {
    const data = JSON.parse(e.data);
    console.log('받은 메시지:', data);
};

ws.onclose = () => {
    console.log('연결 끊김');
    // 재연결 로직 직접 구현 필요
};
```

---

## 5. nginx 설정

### 5.1 SSE를 위한 nginx 설정

```nginx
location /events/ {
    proxy_pass http://backend:8080/;
    
    # SSE 필수 설정
    proxy_buffering off;        # 버퍼링 비활성화
    proxy_cache off;            # 캐시 비활성화
    proxy_read_timeout 600s;    # 타임아웃 설정 (10분)
    proxy_send_timeout 600s;
    
    # 헤더 설정
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    
    # SSE 관련 헤더
    proxy_set_header Connection '';
    proxy_http_version 1.1;
    chunked_transfer_encoding off;
}
```

### 5.2 WebSocket을 위한 nginx 설정

```nginx
location /ws/ {
    proxy_pass http://backend:8080/;
    
    # WebSocket 필수 설정
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    
    # 타임아웃 설정
    proxy_read_timeout 86400s;  # 24시간
    proxy_send_timeout 86400s;
    
    # 헤더 설정
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

### 5.3 설정 비교

| 설정 항목 | SSE | WebSocket |
|-----------|-----|-----------|
| `proxy_buffering` | `off` | 불필요 |
| `proxy_cache` | `off` | 불필요 |
| `proxy_http_version` | `1.1` | `1.1` |
| `Connection` | `''` (빈 문자열) | `"upgrade"` |
| `Upgrade` | 불필요 | `$http_upgrade` |
| `proxy_read_timeout` | 600s (10분) | 86400s (24시간) |

---

## 6. 문제 해결

### 6.1 nginx에서 SSE가 작동하지 않는 경우

**증상:** 이벤트가 수신되지 않음

**원인:** nginx 버퍼링이 활성화되어 있음

**해결:**
```nginx
proxy_buffering off;
proxy_cache off;
```

### 6.2 WebSocket 연결 실패

**증상:** WebSocket handshake 실패

**원인:** Upgrade 헤더 미전달

**해결:**
```nginx
proxy_set_header Upgrade $http_upgrade;
proxy_set_header Connection "upgrade";
```

### 6.3 타임아웃 발생

**증상:** 장시간 연결이 끊김

**원인:** 기본 타임아웃 (60초) 초과

**해결:**
```nginx
proxy_read_timeout 600s;  # 필요한 시간으로 확장
```

---

## 7. sh-platform 적용 사례

### 7.1 현재 구조

```
[브라우저] ←→ [nginx:443] ←→ [Spring Boot:8081]
                ↓
         SSE 엔드포인트
         /scraper/crawl-config/{id}/progress
```

### 7.2 nginx 설정 (현재 적용)

```nginx
location /scraper/ {
    proxy_pass http://127.0.0.1:8081/;
    
    # 기본 설정
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    
    # SSE 지원 설정 (2026-08-04 추가)
    proxy_buffering off;
    proxy_cache off;
    proxy_read_timeout 600s;
    proxy_send_timeout 600s;
}
```

### 7.3 흐름

```
1. 사용자가 "수집 실행" 클릭
2. 프론트엔드: SSE 연결 (EventSource)
3. 프론트엔드: execute API 호출
4. 백엔드: 크롤링 시작
5. 백엔드: SSE 이벤트 전송
6. 프론트엔드: 이벤트 수신 → 알림 표시
```

---

## 8. 벤치마킹

### 8.1 주요 서비스의 채택 현황

| 서비스 | 채택 프로토콜 | 이유 |
|--------|---------------|------|
| Slack | WebSocket | 양방향 채팅 |
| Discord | WebSocket | 실시간 음성/채팅 |
| GitHub | SSE | 알림 피드 |
| Twitter | WebSocket | 실시간 트윗 |
| **SH Platform** | **SSE** | **단방향 알림** ✅ |

---

## 9. 결론

### SSE를 선택한 이유

1. **단순함**: 구현 및 유지보수 용이
2. **필요 충족**: 서버→클라이언트 알림만 필요
3. **자동 재연결**: 네트워크 끊김 시 자동 복구
4. **nginx 호환**: 버퍼링 비활성화로 해결

### 향후 고려사항

- 클라이언트→서버 통신이 필요해지면 WebSocket 전환 검토
- HTTP/2 사용 시 동시 연결 제한 해소

---

*작성일: 2026-08-04*
*작성자: AI Assistant*
