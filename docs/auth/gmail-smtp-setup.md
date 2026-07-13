# Gmail SMTP 설정 가이드

SH Platform 이메일 인증을 위한 Gmail SMTP 설정 방법입니다.

---

## 1. 구글 계정 설정 (웹)

### 1.1 2단계 인증 활성화
> 앱 비밀번호는 2단계 인증이 켜져 있어야 생성 가능합니다.

1. [Google 계정](https://myaccount.google.com/) 접속
2. 왼쪽 메뉴 → **보안**
3. **2단계 인증** → 사용 설정
   - 본인 인증 진행 (휴대폰 등)
4. 완료되면 뒤로 가기

### 1.2 앱 비밀번호 생성
1. 같은 **보안** 페이지
2. 검색창에 `앱 비밀번호` 검색 또는
   [앱 비밀번호](https://myaccount.google.com/apppasswords) 직접 접속
3. **앱 선택** → `기타 (맞춤 이름)`
4. 이름 입력: `sh-platform`
5. **생성**
6. **16자리 앱 비밀번호**가 나타남 → **즉시 복사해서 저장**
   > 이 창을 닫으면 다시 볼 수 없습니다. 분실 시 삭제 후 재생성 필요

---

## 2. 서버에 환경변수 등록

### 2.1 기존 .env 확인
```bash
# WEB VM 접속 후
cat /home/ubuntu/sh-platform/.env
```
현재 `MAIL_USERNAME` / `MAIL_PASSWORD`가 없으면 추가 필요.

### 2.2 .env에 Gmail 정보 추가
```bash
sudo tee -a /home/ubuntu/sh-platform/.env << 'EOF'
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=abcd efgh ijkl mnop
EOF
```
> 앱 비밀번호는 공백 포함 16자리 (예: `abcd efgh ijkl mnop`). 따옴표 없이 그대로 입력.

### 2.3 systemd 서비스에 환경변수 추가
```bash
sudo vi /etc/systemd/system/sh-platform.service
```

`[Service]` 섹션에 아래 2줄 추가:
```
Environment="MAIL_USERNAME=your-email@gmail.com"
Environment="MAIL_PASSWORD=abcd efgh ijkl mnop"
```

### 2.4 서비스 재시작
```bash
sudo systemctl daemon-reload
sudo systemctl restart sh-platform
```

### 2.5 확인
```bash
sudo journalctl -u sh-platform -n 20 --no-pager | grep mail
```
`Verification email sent` 로그가 보이면 성공.

---

## 3. 테스트 (API 호출)

이메일 인증 테스트:
```bash
curl -X POST https://sunghoonyk.duckdns.org/api/v1/auth/verify-email \
  -H "Content-Type: application/json" \
  -d '{"email":"your-email@gmail.com","purpose":"SIGNUP"}'

# → "인증 메일이 발송되었습니다."
```

---

## 4. 문제 해결

| 증상 | 원인 | 해결 |
|------|------|------|
| `Authentication Failed` | 앱 비밀번호 오류 | 재발급 후 다시 등록 |
| `Connection refused` | Gmail SMTP 차단 | OCI 방화벽 587 확인 |
| 메일 안 옴 | 스팸함 확인 | `sh-platform` 발신자 추가 |

---

## 5. 참고

- Gmail 무료 계정은 하루 약 500통 발송 제한
- `MAIL_USERNAME`/`MAIL_PASSWORD`는 systemd 서비스에도 등록해야 Java에서 읽음
- `application-prod.yml`에 SMTP 설정은 이미 되어 있음
