---
title: Health Check YAML Indent + 302 Redirect
description: 배포 워크플로우 헬스체크 실패 원인 분석 및 수정
category: plan
created: 2026-07-21
updated: 2026-07-21
---

## Issue

GitHub Actions `deploy-backend.yml` — `Start services` step 내 헬스체크가 항상 실패하여 배포가 Failure로 끝남.

## 증상

- Run #50 (`961397a`): **Invalid workflow file — yaml syntax on line 171** (annotation만, workflow는 실행됨)
- Run #51 (`7e458dd`): annotation 없었지만 여전히 **Failure** (헬스체크 실패)
- 모든 step은 통과했지만 `Start services` 내 헬스체크 루프가 timeout되어 마지막에 `exit 1`

## 원인 1: YAML Indentation Error (근본 원인)

`deploy-backend.yml` `run: |` 블록 내 Python 코드 들여쓰기가 **0~4칸**이었음.

```
      run: |                           ← block indent = 10
          sshpass -e ssh ... << 'REMOTE'
            ...
              code=$(python3 -c "
import urllib.request          ← indent 0 (YAML ERROR)
try:                            ← indent 0
    resp = urlopen(...)         ← indent 4
" 2>/dev/null)                  ← indent 0
```

YAML `|` literal block scalar는 첫 번째 content line의 들여쓰기(10)보다 **작은 들여쓰기를 가진 라인을 허용하지 않음**. GitHub Actions Parser가 이 라인들을 무시하거나 잘못 파싱하여 `run:` 스크립트가 깨짐.

**결과**: Python 코드가 사실상 실행되지 않음 → `code` 변수에 빈 문자열 할당 → `[ -n "$code" ] && [ "$code" != "ERR" ]` 실패 → 18회 재시도 후 timeout.

### 해결

Python 코드 라인들을 12칸 들여쓰기로 통일 (YAML block indent 10 충족).

## 원인 2: 302 Redirect → OAuth2 Login Timeout

Auth 백엔드의 `SecurityConfig.java`가 `.oauth2Login()` 설정을 포함하여, 미인증 `/api/v1/auth/me` 요청 시 **302 Found**로 외부 OAuth2 Provider 로그인 페이지로 리다이렉트.

`urllib.request.urlopen()`은 **기본적으로 302를 따라감** (redirect following):

1. `/api/v1/auth/me` → **302** → OAuth2 login page (외부 도메인)
2. 외부 도메인 요청이 5초 timeout 초과 → `URLError` 발생
3. `except urllib.error.HTTPError`는 `URLError`를 **잡지 못함**
4. `except:`에서 `'ERR'` 출력 → 헬스체크 실패

```
302 → OAuth2 login (external) → timeout(5s) → URLError → 'ERR'
```

### 해결

`NoRedirect` HTTPRedirectHandler 추가:

```python
class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, hdrs, newurl):
        return None
opener = urllib.request.build_opener(NoRedirect)
```

302를 따라가지 않고 **302 상태코드를 직접 반환** → bash 조건 `[ -n "$code" ]` 통과.

## 최종 조건

모든 HTTP 상태코드(200, 302, 403, ...)를 성공으로 간주:

```bash
if [ -n "$code" ] && [ "$code" != "ERR" ]; then
    echo "Auth up ($code)"
    exit 0
fi
```

## 수정 Commit

`a666b16` — "fix: fix YAML indentation and add NoRedirect handler for health check"

변경 파일: `.github/workflows/deploy-backend.yml`

## 검증

Run #52 (`a666b16`): **Success** ✅ (6m 38s)

## 향후 조치

- Auth의 `.oauth2Login()` 제거 또는 요청 경로별 302/403 분기 검토
- 헬스체크 전용 `GET /api/v1/auth/health` 엔드포인트 추가 고려 (200 고정 반환)
