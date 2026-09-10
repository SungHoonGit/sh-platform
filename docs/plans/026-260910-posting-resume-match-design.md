# 026-260910-posting-resume-match-design 문서

## 개요
- **목적**: 지원 관리에서 공고의 기술 스택과 내 이력서(프로젝트·작업물) 기술 스택을 비교해 일치 기술 배지+개수를 표시
- **범위**: resume 모듈 (매칭 API + ApplicationsPage 표시), scraper 연동 없이 프론트가 공고 tech를 전달
- **작성일**: 2026-09-10
- **작성자**: AI Assistant / 사용자

## 1. 배경 및 이유
- 로드맵 백로그 "공고 키워드-이력서 매칭 점수" 항목
- 방금 완료한 기술 스택 키워드화(`resume_skill_master` + 유사어/별칭 검색)를 기반으로 지원 관리 화면에 매칭 정보 제공
- 사용자 확정: **생략 스코프** — 점수 산식 없이 "일치 기술 배지 + 개수"만 표시, 추후 확장 가능한 구조

## 2. 요구 사항

### 2.1 기능 요구 사항
- [ ] FR-001: 공고의 기술 스택 목록과 내 이력서(프로젝트+작업물) 기술 스택을 마스터 기반으로 정규화해 일치 기술과 개수 반환
- [ ] FR-002: 유사어 매칭 (별칭) — 예: 공고 "React.js" vs 내 "React" → 일치, "자바" vs "Java" → 일치
- [ ] FR-003: ApplicationsPage에서 "스크랩에서 불러오기" 선택 시 해당 공고의 tech와의 일치 기술 배지 표시

### 2.2 비기능 요구 사항
- 점수 산식은 두지 않음 (matchCount만), 추후 확장 지점 열어둠
- DB 신규 테이블/컬럼 없음 — `resume_skill_master`(v12) 재사용

## 3. 설계

### 3.1 매칭 로직
```
내 이력서 기술 (프로젝트.techStack + 작업물.techStack, 콤마 구분 문자열)
   └─ split → trim → 정규화(마스터 name/alias 기준 대소문자 무시) → 표준 기술 집합

공고 기술 (프론트가 scraper 응답의 tech 문자열 전달)
   └─ split → trim → 정규화(동일) → 표준 기술 집합

교집합 = matched 기술 목록
```

### 3.2 API 설계
| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/v1/applications/match-skills` | 공고 기술과 내 이력서 기술 매칭 |

요청:
```json
{ "techStack": "Java, Spring, React.js" }
```
응답:
```json
{
  "matchedSkills": ["Java", "Spring", "React"],
  "matchCount": 3,
  "requestedCount": 3,
  "mySkillCount": 5
}
```

### 3.3 구현 계획
1. `SkillMasterRepository`에 전체 활성 목록 조회(`findByActiveTrue`) 추가
2. `SkillMatchRequest`/`SkillMatchResponse` DTO 신규
3. `SkillMatchService`/`SkillMatchServiceImpl` — 사용자 기준 이력서 기술 수집(Project+PortfolioItem Repository) + 정규화/교집합
4. `ApplicationController`(또는 별도 컨트롤러)에 `POST /match-skills`
5. 단위 테스트 (정규화/유사어/빈 입력)
6. 프론트: ApplicationsPage 스크랩 선택 시 매칭 API 호출 → 일치 배지 표시

---
*작성일: 2026-09-10*