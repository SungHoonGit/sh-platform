---
title: Data Paths
description: Data Paths - scraper module documentation
category: scraper
created: 2026-07-14
updated: 2026-07-21
---

# Scraper Platform - 데이터 경로

> 스크래퍼 데이터 저장 위치 및 관리 규칙

---

## 데이터 경로

### 기본 경로

```
/home/ubuntu/data/
├── scraper/                ← 스크래퍼 수집 데이터
│   ├── java/              ← 자바 채용공고
│   │   ├── 2026-07-08.md
│   │   ├── 2026-07-09.md
│   │   └── ...
│   └── react/             ← 리액트 채용공고
│       ├── 2026-07-08.md
│       └── ...
└── shared/                 ← 공유 데이터 (추후)
```

### 애플리케이션 설정

```yaml
# application.yml
app:
  data:
    base-path: /home/ubuntu/data
    scraper:
      path: /home/ubuntu/data/scraper
      categories:
        - java
        - react
```

---

## 파일 구조

### 날짜별 파일명

```
{category}/{date}.md

예시:
  java/2026-07-14.md
  react/2026-07-14.md
```

### 파일 내용 형식

```markdown
# 2026-07-14 Java 채용공고

## 회사A
- 포지션: 백엔드 개발자
- 기술: Java, Spring Boot
- 경력: 3년 이상
- 위치: 강남구

## 회사B
- 포지션: 풀스택 개발자
- 기술: Java, React
- 경력: 5년 이상
- 위치: 서초구
```

---

## 관리 규칙

### 1. 파일 생성
- 스크래퍼 실행 시 자동 생성
- 날짜별 1개 파일
- 카테고리별 분리

### 2. 파일 수정
- 수동 수정 가능
- 스크래퍼가 덮어쓰지 않음 (주의)

### 3. 파일 백업
- `/home/ubuntu/data/` 전체 백업 권장
- Git 관리 불가 (파일 크기)

### 4. 파일 접근
- Spring Boot에서 직접 읽기
- API를 통해 프론트엔드에 전달

---

## 기존 데이터 마이그레이션

### 마이그레이션 경로

```
기존: /home/ubuntu/job-scraper/daily/
변경: /home/ubuntu/data/scraper/
```

### 마이그레이션 명령어

```bash
# 기존 데이터 복사
cp -r /home/ubuntu/job-scraper/daily/* /home/ubuntu/data/scraper/

# 확인
ls -la /home/ubuntu/data/scraper/
```

---

## 확장

### 새 카테고리 추가

```bash
# 새 카테고리 디렉토리 생성
mkdir -p /home/ubuntu/data/scraper/python

# 설정 파일에 추가
# application.yml
app:
  data:
    scraper:
      categories:
        - java
        - react
        - python  # 추가
```

### 공유 데이터

```
/home/ubuntu/data/shared/
├── config/        ← 공유 설정
├── templates/     ← 템플릿
└── logs/         ← 공유 로그
```
