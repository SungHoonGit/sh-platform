# 005-260807-viewer-search-criteria-display 설계 문서

## 개요
- **목적**: Viewer 헤더에 스케줄 등록된 검색 조건(키워드, 경력, 지역) 표시
- **범위**: 백엔드 CrawlerListController, 프론트 Crawler 타입, Viewer 헤더
- **작성일**: 2026-08-07
- **작성자**: AI Assistant

## 1. 배경 및 이유
- 스케줄 등록 시 설정한 검색 조건(keyword, career, location)이 Viewer에서 확인 불가
- 사용자가 "이 조건으로 수집된 데이터"인지 즉시 인지할 수 없음
- 신규 판정 기준이 일관됨을 시각적으로 확인할 필요성

## 2. 요구 사항
### 2.1 기능 요구 사항
- [x] FR-001: Viewer 헤더에 검색 조건 배지 표시
- [x] FR-002: 스케줄 아이콘 표시
- [x] FR-003: 선택된 크롤러의 검색 조건만 표시

### 2.2 비기능 요구 사항
- 성능: API 응답에 1 필드 추가 (경미한 오버헤드)
- 보안: 기존 인증 체계 유지

## 3. 설계
### 3.1 아키텍처
```
CrawlerListController
  └─ extractSearchCriteria(siteConfigs)
       └─ paramValues JSON 파싱 → {keyword, career, location}

Viewer 헤더
  └─ selectedCrawler.searchCriteria 표시
       └─ 배지(badge) 형태로 깔끔하게 표시
```

### 3.2 데이터 모델
#### 백엔드 응답 추가 필드
```json
{
  "id": 1,
  "name": "Java 시니어",
  "searchCriteria": {
    "keyword": "Java",
    "career": "3~5년",
    "location": "서울"
  }
}
```

#### 프론트 타입 추가
```typescript
interface SearchCriteria {
  keyword?: string;
  career?: string;
  location?: string;
}

interface Crawler {
  // ... 기존 필드
  searchCriteria?: SearchCriteria;
}
```

### 3.3 API 설계
- **GET /scraper/docs/crawlers** 응답에 `searchCriteria` 필드 추가
- `paramValues` JSON에서 `keyword`, `career`, `location` 추출

### 3.4 UI 설계
```
[🤖 Java 시니어] | 2026-08-07 | [keyword: Java] [career: 3~5년] [location: 서울]
                                  [전체] [사람인] [잡코리아] [원티드] [리멤버]
                                  120건  [📥 Excel]
```

## 4. 구현 계획
| 단계 | 내용 | 상태 |
|------|------|------|
| Phase 1 | 백엔드 searchCriteria 필드 추가 | 완료 |
| Phase 2 | 프론트 타입 추가 | 완료 |
| Phase 3 | Viewer 헤더 배지 표시 | 완료 |

## 5. 참고 자료
- 기존 코드: `CrawlerListController.java`, `Viewer.tsx`

---
*작성일: 2026-08-07*