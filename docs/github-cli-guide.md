---
title: GitHub CLI (gh) Guide
description: GitHub CLI 설치, 인증, 주요 사용법
category: config
created: 2026-07-22
updated: 2026-07-22
---

# GitHub CLI (gh) 가이드

## 개요

`gh`는 GitHub을 커맨드라인에서 조작하는 공식 CLI 도구.

| 기능 | 예시 |
|------|------|
| 워크플로우 트리거 | `gh workflow run "Deploy Backend"` |
| PR 생성 | `gh pr create --title "feat: xxx"` |
| 이슈 확인 | `gh issue list` |
| 릴리즈 관리 | `gh release create v1.0.0` |
| 인증 상태 확인 | `gh auth status` |

## 설치

```bash
winget install GitHub.cli
```

설치 후 경로에 추가되므로 **새 터미널**을 열어야 함.

## 인증

```bash
# 브라우저 로그인 (기본)
gh auth login

# 토큰으로 로그인 (CI/서버용)
gh auth login --with-token < token.txt

# 인증 상태 확인
gh auth status
```

`gh auth login` 실행 시:
1. GitHub.com 선택
2. HTTPS 선택
3. 브라우저에서 로그인 또는 토큰 입력
4. `gh` CLI 권한 승인

## sh-platform 주요 사용법

### 워크플로우 트리거

```bash
cd git/sh-platform

# 수동 트리거
gh workflow run "Deploy Backend (Spring Boot)"

# 특정 브랜치에서
gh workflow run "Deploy Backend (Spring Boot)" --ref master

# 실행 결과 확인
gh run list --limit 5
gh run watch
```

### PR 관리

```bash
# 현재 브랜치에서 PR 생성
gh pr create --title "feat: 새 기능" --body "설명"

# PR 목록 확인
gh pr list

# PR 머지
gh pr merge 123 --squash
```

### 이슈 관리

```bash
gh issue list
gh issue create --title "버그" --body "설명"
```

## 토큰 관리

토큰은 `%APPDATA%\GitHub CLI\hosts.yml`에 저장됨.

```
github.com:
    user: SungHoonGit
    oauth_token: ghp_xxxxxxxxxxxx
    git_protocol: https
```

## 참고

- 공식 문서: https://cli.github.com/
- sh-platform 프로젝트: https://github.com/SungHoonGit/sh-platform
