package com.shplatform.resume.domain;

import com.shplatform.resume.api.dto.FileUploadResponse;

/**
 * 파일 저장소 도메인 서비스.
 * 실제 바이너리는 로컬 디스크({app.resume.upload-dir})에 저장하고 메타데이터는 DB에 보관한다.
 */
public interface FileStorageService {

    /**
     * (명령형) 파일을 업로드한다.
     *
     * @param userId           소유 사용자 ID
     * @param originalFilename 원본 파일명
     * @param contentType      MIME 타입
     * @param data             파일 바이트
     * @return 업로드된 파일 정보
     * @throws BusinessException INVALID_INPUT  빈 파일 또는 허용되지 않은 확장자
     * @throws BusinessException PAYLOAD_TOO_LARGE 최대 크기(10MB) 초과
     */
    FileUploadResponse upload(Long userId, String originalFilename, String contentType, byte[] data);

    /**
     * (질의형) 본인이 업로드한 파일을 다운로드한다.
     *
     * @param userId 요청 사용자 ID
     * @param fileId 파일 ID
     * @return 원본 파일명/컨텐츠 타입/바이너리
     * @throws BusinessException NOT_FOUND 파일 메타데이터 또는 실체 파일이 없을 때
     * @throws BusinessException FORBIDDEN 소유자가 아닐 때
     */
    DownloadedFile download(Long userId, Long fileId);

    /**
     * 다운로드 결과 값 객체.
     *
     * @param originalName 원본 파일명
     * @param contentType  MIME 타입
     * @param data         파일 바이너리
     */
    record DownloadedFile(String originalName, String contentType, byte[] data) {
    }
}
