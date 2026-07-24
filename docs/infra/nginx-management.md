---
title: Nginx 설정 관리 방식
description: MSA/MPA 아키텍처에서 nginx 리버스 프록시 설정을 어떻게 관리할지에 대한 분석
category: infra
created: 2026-07-23
updated: 2026-07-23
---

# Nginx 설정 관리 방식

## 현재 상태

### 문제점

1. **`fix-nginx.py` 방식의 한계**: regex로 설정 파일을 패치하는 방식 → 구조가 복잡해지면 깨지기 쉬움
2. **서버-git 불일치**: 서버에서 직접 수정하면 git과 동기화 안 됨
3. **CI/CD 충돌**: `git reset --hard`로 코드는 동기화되지만, 서버의 nginx 설정은 별도 관리됨
4. **중복 location**: `fix-nginx.py`가 기존 블록을 제대로 제거하지 못해 중복 발생 (2026-07-22 이슈)

### 현재 구조

```
CI/CD (deploy-backend.yml)
  → git pull (서버)
  → 프론트엔드 빌드
  → fix-nginx.py 실행 (regex 패치)
  → nginx -t && reload
```

## 대안 분석

### 방식 1: nginx 설정을 git에 직접 관리 (추천)

**개념**: nginx 설정 파일 자체를 git 저장소에 넣고, CI/CD에서 서버로 복사

```
nginx/
├── sh-platform.conf     # 메인 서버 블록
├── ssl/                 # (git 제외, 서버에만)
└── snippets/            # 재사용 가능한 설정
    ├── proxy-params.conf
    └── ssl-params.conf
```

**장점:**
- 설정 변경이 git 커밋으로 추적됨
- `fix-nginx.py` 같은 해킹 스크립트 불필요
- 다른 환경(staging 등)으로도 동일 설정 적용 가능
- 설정 충돌 시 `git diff`로 확인 가능

**단점:**
- SSL 인증서 경로 등 환경별 차이는 `.env`나 템플릿으로 처리 필요

**적합성**: ⭐⭐⭐⭐⭐ (서버 1~3개 규모, 소규모 팀)

---

### 방식 2: Ansible로 설정 자동화

**개념**: 설정 파일을 템플릿으로 관리하고, Ansible 플레이북으로 서버에 배포

```yaml
# ansible/nginx.yml
- hosts: web
  tasks:
    - name: Deploy nginx config
      template:
        src: nginx/sh-platform.conf.j2
        dest: /etc/nginx/sites-enabled/sh-platform
      notify: reload nginx
```

**장점:**
- 서버 수가 늘어나면 유용 (웹 서버 3개 이상)
- 설정 변경 이력 관리
- 롤백 기능

**단점:**
- 학습 곡선 존재
- 서버 1개에는 오버헤드

**적합성**: ⭐⭐⭐ (서버 3개 이상, 팀 5명 이상)

---

### 방식 3: Docker + nginx 리버스 프록시

**개념**: nginx를 컨테이너로 올리고 설정을 볼륨 마운트

```yaml
# docker-compose.yml
services:
  nginx:
    image: nginx:alpine
    volumes:
      - ./nginx/sh-platform.conf:/etc/nginx/conf.d/default.conf
    ports:
      - "80:80"
      - "443:443"
```

**장점:**
- 설정이 컨테이너 이미지에 포함
- 배포가 완전히 격리됨
- 로컬 개발 환경과 프로덕션 동일

**단점:**
- OCI A1.Flex에서 Docker 오버헤드 고려 필요
- SSL 인증서 마운트 관리
- 기존 Spring Boot 앱과의 네트워크 구성 변경

**적합성**: ⭐⭐⭐ (OCI 컨테이너 서비스 사용 시, 전체 MSA 전환 시)

---

### 방식 4: OCI DevOps + Container Registry

**개념**: OCI의 DevOps 서비스와 컨테이너 레지스트리를 사용

**장점:**
- OCI 네이티브 통합
- 이미지 버전 관리
- 자동 배포 파이프라인

**단점:**
- OCI 플랫폼 종속
- 현재 A1.Flex VM 방식과 구조 변경 필요
- 비용 발생 가능

**적합성**: ⭐⭐ (OCI 풀스택 전환 시)

---

## 추천 방식

### 현재 규모 (서버 1개, 팀 1~3명)

**방식 1 (git 직접 관리) + 기존 CI/CD 유지**

변경 사항:
1. `nginx/sh-platform.conf` 파일을 git에 저장
2. `fix-nginx.py` → 설정 파일 복사 + `nginx -t`로 대체
3. `deploy-backend.yml`에서 nginx 배포 단계 수정

```yaml
# 변경 후 nginx 배포 단계
- name: Deploy nginx config
  run: sshpass -e ssh ... "cd /home/ubuntu/sh-platform && sudo cp nginx/sh-platform.conf /etc/nginx/sites-enabled/sh-platform && sudo nginx -t && sudo systemctl reload nginx"
```

### 향후 확장 시

서버가 3개 이상으로 늘어나면:
- 방식 2 (Ansible)로 전환
- 또는 방식 3 (Docker)로 전환 검토

## 참고 자료

- [nginx 공식 문서](https://nginx.org/en/docs/)
- [Ansible nginx 역할](https://galaxy.ansible.com/nginxinc/nginx)
- [Docker nginx 이미지](https://hub.docker.com/_/nginx)
- [OCI 컨테이너 서비스](https://docs.oracle.com/en-us/iaas/Content/Concepts/containerregistry.htm)
