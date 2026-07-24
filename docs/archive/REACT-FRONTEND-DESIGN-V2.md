---
title: React Frontend Design v2
description: React Frontend Design v2 - general module documentation
category: front
created: 2026-07-16
updated: 2026-07-21
---

# React 프론트엔드 설계 V2

## 페이지 구성 (3페이지)

### 1. 통합검색 (`/search`)
- 검색어 입력
- 카테고리 필터 (左侧)
- 사이트 선택 체크박스
- 검색 결과 테이블
- "이 검색을 스케줄로 등록" 버튼

### 2. 스케줄 등록 (`/schedule`)
- 스케줄 이름
- 검색 조건 (키워드, 경력, 지역)
- 사이트 선택
- 스케줄 시간 설정 (cron)
- 저장/수정/삭제

### 3. 뷰어 (`/viewer`)
- 파일 탐색기 (左侧)
- 채용공고 그리드 (右侧)
- 사이트 탭 필터
- 페이징
