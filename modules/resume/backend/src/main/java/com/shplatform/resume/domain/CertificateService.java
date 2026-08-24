package com.shplatform.resume.domain;

import com.shplatform.resume.api.dto.CertificateRequest;
import com.shplatform.resume.api.dto.CertificateResponse;

import java.util.List;

/**
 * 자격증 도메인 서비스.
 */
public interface CertificateService {

    /**
     * (질의형) 내 자격증 목록을 표시 순서대로 조회한다.
     *
     * @param userId 로그인 사용자 ID
     * @return 자격증 목록 (display_order ASC, id ASC)
     */
    List<CertificateResponse> getCertificates(Long userId);

    /**
     * (명령형) 자격증을 추가한다.
     *
     * @param userId  로그인 사용자 ID
     * @param request 자격증 정보
     * @return 생성된 자격증
     */
    CertificateResponse createCertificate(Long userId, CertificateRequest request);

    /**
     * (명령형) 자격증을 수정한다.
     *
     * @param userId        로그인 사용자 ID
     * @param certificateId 자격증 ID
     * @param request       수정할 자격증 정보
     * @return 수정된 자격증
     * @throws BusinessException NOT_FOUND 자격증이 없을 때, FORBIDDEN 다른 사용자의 자격증일 때
     */
    CertificateResponse updateCertificate(Long userId, Long certificateId, CertificateRequest request);

    /**
     * (명령형) 자격증을 삭제한다.
     *
     * @param userId        로그인 사용자 ID
     * @param certificateId 자격증 ID
     * @throws BusinessException NOT_FOUND 자격증이 없을 때, FORBIDDEN 다른 사용자의 자격증일 때
     */
    void deleteCertificate(Long userId, Long certificateId);
}
