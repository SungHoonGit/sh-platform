# 015-260922-cron-d-missing-newline 오류 기록

## 개요
- **발생일**: 2026-09-17 ~ 2026-09-22 (설치일부터 약 5일간 주기 백업 전면 미실행)
- **환경**: Ubuntu 24.04 (OCI WEB 140.245.95.162), Debian cron (`/usr/sbin/cron -f -P`)
- **심각도**: 🔴 Critical (주기 백업 스케줄이 파일 전체 무시 상태로 전혀 동작하지 않음)

## 1. 오류 현상
### 1.1 에러 메시지
```
Sep 17 09:50:07 instance-... cron[2026557]: (*system*sh-platform-mysql-backup) ERROR (Missing newline before EOF, this crontab file will be ignored)
Sep 17 10:00:01 instance-... cron[2026557]: (*system*sh-platform-mysql-backup) RELOAD (/etc/cron.d/sh-platform-mysql-backup)
Sep 17 10:00:01 instance-... cron[2026557]: (*system*sh-platform-mysql-backup) ERROR (Missing newline before EOF, this crontab file will be ignored)
... (매 deploy마다 RELOAD → ERROR 반복)
```

### 1.2 재현/발견 단계
1. 9/17에 cron 패키지 설치 + cron.d 파일 배포 (수동 실행 1회 검증 성공)
2. 9/18 아침 03:30 예정 첫 자동 실행 결과 확인 → 산출물 없음
3. 9/22 배포 워크플로우 진단 + SSH 전용 경량 진단 워크플로우(`diag-ssh.yml`)로 syslog/journal 확인
4. `journalctl -u cron | grep sh-platform`에서 위 ERROR 발견, `cat -A`로 마지막 줄에 `$`(개행) 없음 확인

## 2. 원인 분석
### 2.1 근본 원인
`infra/cron.d/sh-platform-mysql-backup` 파일이 **마지막 줄 개행(`\n`) 없이 EOF로 끝남**.

Debian/Ubuntu cron은 cron.d 파일의 마지막 줄이 개행으로 끝나지 않으면
`Missing newline before EOF` 오류를 내고 **그 파일 전체를 무시한다** (일부 줄만 실행하는 게 아님).

- Git의 `eol=lf` 속성은 기존 개행을 LF로 만들 뿐, **부재한 마지막 개행을 추가하지 않음**
- `systemctl is-active cron` → active, 수동 스크립트 실행 → 성공 이라서 진단이 어려웠음
- cron 데몬 자체 로그(`journalctl -u cron`)만 잘 보면 즉시 발견 가능한 상태였음

### 2.2 관련 코드
- 파일: `infra/cron.d/sh-platform-mysql-backup` (수정 전 마지막 바이트 `3E 26 31` = `2>&1`, `0A` 없음)

## 3. 해결 방법
### 3.1 해결 과정
1. 소스 파일에 마지막 개행 추가 + 주의 주석 기록
2. 배포 워크플로우에 **개행 가드**(멱등) 추가 — 소스와 무관하게 배포 시점에 방지:
   ```bash
   sudo sh -c 'test "$(tail -c1 /etc/cron.d/sh-platform-mysql-backup)" || echo >> /etc/cron.d/sh-platform-mysql-backup'
   ```
3. 검증: 재배포 후 `journalctl -u cron --since "-3 minutes"`에 RELOAD 후 **ERROR 없음** 확인 + `cat -A` 마지막 줄 `$` 확인
4. 누락 기간(9/18~9/21) 백업은 복구 불가 → 9/22 수동 실행으로 현재 시점 스냅샷 확보. binlog(9/17 활성화) 체인으로 그 사이 변경분은 이론적 복구 가능

## 4. 예방 방법
- [x] 배포 워크플로우 개행 가드 (소스 회귀 무관)
- [x] cron.d 파일에 주의 주석
- [x] 가이드 009 §6 문제 해결 표에 등재
- [x] 검증 도구: `diag-ssh.yml`(SSH 전용 경량 진단) — cron RELOAD/ERROR 즉시 확인 가능
- 주의: 텍스트 설정파일(crontab/systemd drop-in 등 **파일 전체를 파싱하는 포맷**)은 마지막 개행 필수

## 5. 참고 자료
- 진단 run: `gh run view 35674023923` (diag-ssh.yml 첫 실행)
- 가이드: `docs/guides/009-260914-mysql-backup-guide.md` §6

---
*작성일: 2026-09-22*
