# 문서 뷰어 서비스 구현 계획

## 1. 개요

스크래퍼가 수집한 MD 파일(구인 공고)을 웹 브라우저에서 열람, 검색, 내보내기할 수 있는 서비스.

### 목표
- 파일 트리 기반 디렉토리 탐색
- MD 파일 렌더링 (표, 코드 블록, 링크 지원)
- 전체 텍스트 검색
- PDF/Excel 내보내기
- 로컬 경로 설정 가능 (crawl_config.localPath)

### 기술 스택
| 구성요소 | 기술 |
|---------|------|
| 백엔드 | Java Spring Boot |
| 프론트엔드 | Thymeleaf + Alpine.js |
| MD 파싱 | flexmark-java |
| PDF 생성 | openpdf |
| Excel 생성 | apache-poi |
| PDF 뷰어 | PDF.js (추후) |

## 2. 아키텍처

### 디렉토리 구조
```
sh-platform-common/src/main/java/com/shplatform/common/file/
├── model/
│   └── FileNode.java              # 파일/디렉토리 트리 노드
├── service/
│   ├── FileTreeService.java       # 디렉토리 스캔 → 트리
│   ├── FileReadService.java       # MD 읽기 + HTML 렌더링
│   ├── FileSearchService.java     # 전체 텍스트 검색
│   └── FileExportService.java     # PDF/Excel 변환
└── controller/
    └── FileController.java        # REST API

scraper-platform-backend/
├── templates/docs/
│   └── viewer.html                # 뷰어 페이지
└── resources/static/docs/
    └── style.css                  # 스타일
```

### 데이터 흐름
```
사용자 → 브라우저 → nginx → scraper-platform (port 8081)
                                   │
                                   ├─ FileController
                                   │    ├─ GET /docs/tree      → FileTreeService
                                   │    ├─ GET /docs/file      → FileReadService
                                   │    ├─ GET /docs/search    → FileSearchService
                                   │    └─ GET /docs/export/*  → FileExportService
                                   │
                                   └─ crawl_config.localPath 기준 스캔
```

## 3. API 설계

### 3.1 파일 트리
```
GET /docs/tree?configId=1
```
응답:
```json
{
  "root": "/home/ubuntu/job-scraper/daily/java",
  "nodes": [
    {
      "name": "2026-07-14.md",
      "path": "2026-07-14.md",
      "type": "file",
      "size": 2004,
      "modifiedAt": "2026-07-14T11:47:00"
    },
    ...
  ]
}
```

### 3.2 파일 읽기
```
GET /docs/file?configId=1&path=2026-07-14.md&format=html
```
- `format=raw`: MD 원본 텍스트 반환
- `format=html`: 렌더링된 HTML 반환

### 3.3 검색
```
GET /docs/search?configId=1&q=백엔드
```
응답:
```json
{
  "query": "백엔드",
  "totalMatches": 15,
  "results": [
    {
      "file": "2026-07-14.md",
      "line": 12,
      "content": "| (주)써로마인드 | saramin | Backend Developer | ...",
      "highlight": "...<mark>백엔드</mark> 개발자..."
    }
  ]
}
```

### 3.4 내보내기
```
GET /docs/export/pdf?configId=1&path=2026-07-14.md
GET /docs/export/excel?configId=1&path=2026-07-14.md
```

## 4. DB 변경

### crawl_config 테이블 ALTER
```sql
ALTER TABLE crawl_config
ADD COLUMN local_path VARCHAR(500) DEFAULT NULL
AFTER retention_days;
```

### 기존 레코드 업데이트
```sql
UPDATE crawl_config SET local_path = '/home/ubuntu/job-scraper/daily/java' WHERE id = 1;
```

## 5. 보안

### 경로 조작 방지
```java
public class FileTreeService {
    private final Path basePath;
    
    public FileTreeService(String rootPath) {
        this.basePath = Paths.get(rootPath).toRealPath();
    }
    
    public List<FileNode> scan(String relativePath) {
        Path resolved = basePath.resolve(relativePath).normalize();
        if (!resolved.startsWith(basePath)) {
            throw new SecurityException("Path traversal detected");
        }
        // 스캔 로직
    }
}
```

### 허용 확장자
- `.md` 파일만 읽기 가능
- 디렉토리는 자유롭게 탐색 가능

## 6. 구현 단계

### Phase 1: 백엔드 모듈 (sh-platform-common)
1. FileNode 모델 정의
2. FileTreeService 구현
3. FileReadService 구현 (MD → HTML)
4. FileSearchService 구현
5. FileExportService 구현 (PDF/Excel)
6. FileController 구현

### Phase 2: 프론트엔드 (scraper-platform)
1. viewer.html 템플릿 생성
2. 파일 트리 사이드바 (Alpine.js)
3. MD 렌더링 뷰
4. 검색 바
5. 내보내기 버튼

### Phase 3: 연동
1. crawl_config.localPath 필드 추가
2. DB 마이그레이션
3. 전체 테스트

## 7. 테스트 시나리오

1. 파일 트리가 올바르게 표시되는지
2. MD 파일이 HTML로 렌더링되는지
3. 검색이 정확한 결과를 반환하는지
4. PDF/Excel 다운로드가 정상 동작하는지
5. 경로 조작이 차단되는지
6. localPath 변경 시 트리가 갱신되는지

## 8. 참고사항

### 현재 파일 구조
```
/home/ubuntu/job-scraper/
├── config.java.json        # Java 스크래퍼 설정
├── config.react.json       # React 스크래퍼 설정
├── daily/
│   ├── java/
│   │   ├── 2026-07-08.md
│   │   ├── ...
│   │   └── 2026-07-14.md
│   └── react/
│       └── 2026-07-08.md ...
└── job_scraper.py          # 메인 스크래퍼
```

### MD 파일 형식
```markdown
# 2026-07-14 수집 — Java Spring 백엔드 개발자 서울 경기

| 회사명 | 사이트 | 제목 | 경력 | 마감일 | 기술스택 | 기업정보 |
|---|---|---|---|---|---|---|
| (주)써로마인드 | saramin | [Backend Developer](url) | 경력 5년↑ | ~07.31 | 백엔드/서버개발 | - |
```
