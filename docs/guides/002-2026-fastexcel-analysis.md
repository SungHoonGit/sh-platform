# FastExcel 상세 분석

## 1. 개요

FastExcel은 Alibaba EasyExcel의 원작자(Chive)가 독립하여 만든 Java Excel 라이브러리. 기존 EasyExcel의 단점을 보완하고 성능을 대폭 향상시킨 차세대 솔루션.

| 항목 | 내용 |
|------|------|
| Maven Central | `cn.idev.excel:fastexcel` |
| 최신 버전 | 1.2.0 (2024) |
| Java | 11+ (21 완전 호환) |
| 라이선스 | Apache 2.0 |
| GitHub | [dhatim/fastexcel](https://github.com/dhatim/fastexcel) - 908 stars |

---

## 2. 기존 라이브러리 대비 장점

### 2.1 Apache POI 대비

| 항목 | POI (SXSSF) | FastExcel | 개선율 |
|------|-------------|-----------|--------|
| 100K 행 쓰기 | 2,453ms | 309ms | **8배** |
| 100K 행 읽기 | 1,097ms | 210ms | **5배** |
| 코드량 | 25줄+ | 5줄+ | **80% 절감** |
| 메모리 | 높음 | 낮음 | 스트리밍 |

### 2.2 EasyExcel 대비

| 항목 | EasyExcel | FastExcel | 개선 |
|------|-----------|-----------|------|
| 쓰기 속도 | 542ms | 309ms | **1.7배** |
| 읽기 속도 | 334ms | 210ms | **1.6배** |
| PDF 변환 | 미지원 | **지원** | 신규 |
| 유지보수 | 유지보수 모드 | **활발** | 안정 |

---

## 3. 핵심 기능

### 3.1 기본 내보내기 (Writing)

```java
// 1줄로 끝나는 내보내기
EasyExcel.write(response.getOutputStream(), JobPostingVO.class)
         .sheet("채용공고")
         .doWrite(dataList);
```

### 3.2 어노테이션 기반 매핑

```java
@Data
public class JobPostingVO {
    @ExcelProperty(value = "회사명", index = 0)
    private String company;

    @ExcelProperty(value = "포지션", index = 1)
    private String position;

    @ExcelProperty(value = "경력", index = 2)
    private String career;

    @ExcelProperty(value = "기술스택", index = 3)
    private String tech;

    @ExcelProperty(value = "지역", index = 4)
    private String location;

    @ExcelProperty(value = "URL", index = 5)
    private String url;

    @ExcelProperty(value = "사이트", index = 6)
    private String siteName;

    @ExcelProperty(value = "수집일", index = 7)
    @DateTimeFormat("yyyy-MM-dd")
    private LocalDate crawledAt;
}
```

### 3.3 커스텀 헤더

```java
// 복합 헤더
@Data
public class ComplexHeaderVO {
    @ExcelProperty({"기본정보", "회사명"})
    private String company;

    @ExcelProperty({"기본정보", "포지션"})
    private String position;

    @ExcelProperty({"추가정보", "경력"})
    private String career;
}
```

### 3.4 셀 스타일링

```java
EasyExcel.write(out, JobPostingVO.class)
         .registerWriteHandler(new CellWriteHandler() {
             @Override
             public void afterCellDispose(CellWriteHandlerContext context) {
                 // 셀 스타일 커스터마이징
             }
         })
         .sheet("채용공고")
         .doWrite(data);
```

### 3.5 대용량 스트리밍 (10만 행+)

```java
// 페이징 내보내기
EasyExcel.write(out, JobPostingVO.class)
         .sheet("대량데이터")
         .doWrite(new AnalysisContext() -> {
             int rowIndex = context.readRowHolder().getRowIndex();
             List<JobPostingVO> pageData = service.getPageData(rowIndex);
             return pageData; // 마지막 페이지 시 null 반환
         });
```

### 3.6 독립 스트림 API (FastExcel 고유)

```java
// 독립 스트림 API - 더 빠르고 간결
try (FastExcelwriter = FastExcel.writer(out).build()) {
    WriteSheet sheet = EasyExcel.writerSheet("채용공고").build();
    writer.write(dataList, sheet);
}
```

---

## 4. Spring Boot 통합

### 4.1 의존성 (Gradle)

```kotlin
// build.gradle.kts
dependencies {
    implementation("cn.idev.excel:fastexcel:1.2.0")
}
```

### 4.2 Spring Boot Starter (선택)

```kotlin
// pig-mesh starter 사용 시
dependencies {
    implementation("com.pig4cloud.excel:excel-spring-boot-starter:3.4.2")
}
```

### 4.3 REST Controller 예제

```java
@RestController
@RequestMapping("/api/v1/job-postings")
@Tag(name = "Job Postings", description = "채용공고 API")
public class JobPostingExportController {

    private final JobPostingRepository repository;

    @GetMapping("/export")
    @Operation(summary = "채용공고 엑셀 내보내기")
    public void exportExcel(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String siteName,
            HttpServletResponse response) throws IOException {

        // 1. 데이터 조회
        List<JobPosting> postings = repository.findByFilters(keyword, siteName);

        // 2. VO 변환
        List<JobPostingVO> voList = postings.stream()
            .map(this::toVO)
            .toList();

        // 3. 응답 헤더 설정
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String fileName = URLEncoder.encode("채용공고_" + LocalDate.now(), "UTF-8")
            .replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        // 4. Excel 내보내기 (1줄!)
        EasyExcel.write(response.getOutputStream(), JobPostingVO.class)
                 .sheet("채용공고")
                 .doWrite(voList);
    }
}
```

---

## 5. 주의사항

### 5.1 의존성 충돌

```kotlin
// 기존 POI와 공존 시
dependencies {
    implementation("cn.idev.excel:fastexcel:1.2.0") {
        exclude(group = "org.apache.poi", module = "poi-ooxml")
    }
}
```

### 5.2 메모리 관리

```java
// 대용량 시 스트리밍 필수
EasyExcel.write(out, JobPostingVO.class)
         .inMemory(false)  // 스트리밍 모드 (기본값)
         .sheet("대량데이터")
         .doWrite(data);
```

### 5.3 타입 안전성

```java
// 날짜/숫자 타입 주의
@Data
public class SafeVO {
    @ExcelProperty("날짜")
    @DateTimeFormat("yyyy-MM-dd")
    private LocalDate date;  // String 대신 LocalDate 사용

    @ExcelProperty("금액")
    @NumberFormat("#,###.##")
    private BigDecimal amount;
}
```

---

## 6. 벤치마크 결과

### 쓰기 성능 (100K 행)

```
FastExcel   ████████░░░░░░░░░░░░░░░░░░░░░░  309ms
Sheetz      ██████████░░░░░░░░░░░░░░░░░░░░  423ms
EasyExcel   █████████████░░░░░░░░░░░░░░░░░  542ms
Apache POI  ██████████████████████████████  2,453ms
```

### 읽기 성능 (100K 행)

```
FastExcel   █████░░░░░░░░░░░░░░░░░░░░░░░░░  210ms
EasyExcel   ████████░░░░░░░░░░░░░░░░░░░░░  334ms
Apache POI  ██████████████████████████░░░░  1,097ms
Poiji       █████████████████████████░░░░  1,042ms
```

---

## 7. 결론

### 우리 프로젝트에 FastExcel이 맞는 이유

1. **단순 내보내기**: 채용공고 목록을 엑셀로 내보내는 것이 주 용도
2. **수백~수천 행**: 10만 행 이상 대량 처리는 거의 없음
3. **코드 간결성**: 1줄 내보내기로 유지보수 용이
4. **성능 여유**: 기존 POI 대비 8배 빠름 (대량 처리 시 체감)
5. **Java 21 + Spring Boot 3.4 완전 호환**
6. **POI 공존**: 기존 common 모듈의 POI 코드와 충돌 없음

### 리스크 평가

| 리스크 | 수준 | 대응 |
|--------|------|------|
| 라이브러리 미성숙 | 중 | 단순 기능 위주로 제한적 사용 |
| 문서 부족 | 중 | 공식 예제 + 직접 테스트 |
| 커뮤니티 작음 | 하 | 필요 시 POI로 전환 가능 |

### 다음 단계

1. `fastexcel` 의존성 추가
2. VO 클래스 정의 (`JobPostingVO`)
3. Controller 엔드포인트 구현
4. 테스트 + 빌드 검증
5. 배포
