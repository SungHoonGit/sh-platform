# 016-260930-ddl-v14-after-comment-1064-error 오류 기록

## 개요
- **발생일**: 2026-09-30
- **환경**: Ubuntu(Linux) CI, MariaDB 10.11.14, GitHub Actions appleboy/ssh-action
- **심각도**: 🟡 Warning (배포 Deploy 단계 1회 실패, 빌드/테스트는 통과 — 수정 후 즉시 재배포 성공)

## 1. 오류 현상
### 1.1 에러 메시지
```
ERROR 1064 (42000) at line 8: You have an error in your SQL syntax;
check the manual that corresponds to your MariaDB server version for the right
syntax to use near 'COMMENT '키워드 매칭 방식 (exact=정확일치, contains=부분일치)'' at line 4
```
- 위치: `ddl-v14.sql`의 `ALTER TABLE company_blacklist ADD COLUMN ...` 문장
- CI: `Deploy via SSH` 단계 실패(run 36653610286) — `Build and Test All Modules`은 정상 통과

### 1.2 재현 단계
1. `docs/scraper/ddl-v14.sql` 작성 (해당 문장)
2. push → deploy-backend.yml이 서버에서 `mysql ... < docs/scraper/ddl-v14.sql` 실행
3. ERROR 1064

## 2. 원인 분석
### 2.1 근본 원인
`ALTER TABLE ... ADD COLUMN ... column_definition [FIRST | AFTER col_name]`에서
**`FIRST`/`AFTER`는 column_definition의 바깥(가장 마지막) 위치**여야 한다.
`AFTER` 뒤에 `COMMENT '...'`을 두면 MariaDB는 `AFTER`에서 구문이 끝난 것으로 파싱하고
예상치 못한 `COMMENT` 토큰에서 1064를 발생시킨다.

### 2.2 관련 코드
- 파일: `docs/scraper/ddl-v14.sql:8-11`
- 코드(실패):
```sql
ALTER TABLE company_blacklist
    ADD COLUMN IF NOT EXISTS match_type ENUM('exact', 'contains') NOT NULL DEFAULT 'exact'
    AFTER company_name_normalized          -- ← AFTER가 먼저 오면 COMMENT는 허용되지 않음
    COMMENT '키워드 매칭 방식 ...';
```

## 3. 해결 방법
### 3.1 해결 과정
`COMMENT`를 `AFTER` 앞으로 이동 → commit `22c85c7` → push → 재배포 run 36654362254 success.
DDL 멱등(`IF NOT EXISTS`)이라 실패한 1차 실행도 컬럼이 생기지 않은 상태여서 재실행 안전.

### 3.2 최종 코드 변경
```sql
-- 변경 전
    AFTER company_name_normalized
    COMMENT '키워드 매칭 방식 (exact=정확일치, contains=부분일치)';

-- 변경 후
    COMMENT '키워드 매칭 방식 (exact=정확일치, contains=부분일치)'
    AFTER company_name_normalized;
```

## 4. 예방 방법
- DDL 작성 규칙: **`COMMENT` 등 컬럼 속성은 `AFTER`/`FIRST` 앞에** 둔다 (ALTER ADD COLUMN 문법 순서).
- CI가 배포 전 DDL을 서버 mysql로 실행하므로, 문법 오류는 Build/Test 통과 후에도 Deploy가 실패할 수 있음 →
  배포 후 `gh run view --log-failed`의 Deploy 단계까지 확인할 것.
- 멱등 DDL(`IF NOT EXISTS`)이므로 1064 후 재실행은 안전 — 단 `ADD COLUMN`이 아닌 `UPDATE`/`ALTER` 중간 실패는 상태 확인 필요.

## 5. 참고 자료
- MariaDB `ALTER TABLE` 문법: `ADD COLUMN col definition [FIRST | AFTER col]` (AFTER는 마지막)
- 관련: ddl-v14.sql (036 차단 키워드 부분일치)

---
*작성일: 2026-09-30*
