# 019-261001-resume-attachment-preview-404-error 오류 기록

## 개요
- **발생일**: 2026-10-01 (사용자 보고: "첨부 등도 막 오류 나고 힘들었다")
- **환경**: resume 프론트 첨부 미리보기 (라이브)
- **심각도**: 🔴 Critical — 첨부 미리보기가 항상 404로 표시 안 됨

## 1. 오류 현상
### 1.1 증상
- 이력서/항목 폼의 첨부 파일 **미리보기 이미지가 표시되지 않음** (이미지 영역 공백)
- 사용자는 "첨부가 오류 난다"로 보고 (업로드/다운로드 자체와 구분 필요)

### 1.2 라우팅 검증 (진행 중 확인한 것)
- 업로드: 정상 — 서버 `uploads/`에 파일 적재 확인(일일 백업 uploads tar 924K)
- 다운로드(`apiDownload`): 정상 경로 `/resume/api/v1/files/...`
- **미리보기만 404**: 요청 경로가 `/resume/files/1/download` (프리픽스 `/api/v1` 누락)

## 2. 원인 분석
### 2.1 근본 원인
`CrudSection.tsx:58`, `templates/shared.tsx:110`의 미리보기가
```ts
apiFetch(`/resume${fileDownloadPath(path)}`)   // path = "/api/v1/files/1/download"
```
`fileDownloadPath`는 **`/api/v1` 프리픽스를 제거하는 헬퍼**(`client.ts:177`)인데,
이미 `/resume`을 붙이는 자리에서 다시 제거하면 요청이 `/resume/files/1/download`가 되고
nginx `location /resume/` rewrite 후 BE에 **`/files/1/download`로 전달 → `/api/v1/files/...` 매칭 실패 404**.

- `fileDownloadPath`의 목적은 `apiDownload`(base가 `/resume/api/v1`)에 상대경로를 남기기 위함 → **다운로드엔 정상, 미리보기(`apiFetch`)엔 오용**
- 같은 파일이라도 다운로드는 성공, 미리보기만 실패해 "첨부가 일부만 오류"로 인지됨

### 2.2 관련 코드
- `modules/resume/frontend/src/components/CrudSection.tsx:58` (미리보기)
- `modules/resume/frontend/src/components/templates/shared.tsx:110` (미리보기)
- `modules/resume/frontend/src/api/client.ts:177` `fileDownloadPath`

## 3. 해결 방법
### 3.1 해결 과정
`fileDownloadPath` 호출 제거 → `/resume${path}` = `/resume/api/v1/files/...`로 복원.
다운로드·공유 토큰 이미지는 원래 정상이라 불변.

### 3.2 최종 코드 변경
```ts
// 변경 전
apiFetch(`/resume${fileDownloadPath(path)}`, { redirectOn401: true, hashRoute: true })
// 변경 후
apiFetch(`/resume${path}`, { redirectOn401: true, hashRoute: true })
```

## 4. 예방 방법
- 프리픽스 변환 헬퍼는 **"어느 base에 붙이는가"와 짝을 지어** 사용할 것: `apiDownload`(base `/resume/api/v1`)에는 `fileDownloadPath` 정상, `apiFetch`(절대경로)에는 상대경로 변환 불필요
- 경로 결합은 한 곳(API_BASE 상수)에서만 만들 것 — 로컬 문자열 합성(`/resume${...}`)은 라우팅 이중화로 이어짐
- 동일 첨부라도 **미리보기·다운로드·공유 3경로를 모두 라이브에서 검증**할 것 (한 경로만 검증하면 이번처럼 놓침)

## 5. 참고 자료
- 커밋 `839b141`, deploy run 36802788979
- 관련: `docs/errors/017-260930-resume-service-arg-order-reversed-error.md` (같은 폼의 수정 오류 — 별개 원인)

---
*작성일: 2026-10-01*
