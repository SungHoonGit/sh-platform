package com.shplatform.resume.domain;

import com.shplatform.resume.api.dto.ShareLinkResponse;
import com.shplatform.resume.api.dto.ShareViewResponse;
import java.util.Optional;

/**
 * 이력서 문서 공유 링크 도메인 서비스.
 */
public interface ResumeShareService {

    /**
     * (명령형) 문서 공유 링크를 생성한다. 이미 공유 링크가 있으면 새 토큰으로 재발급한다.
     *
     * @param userId     로그인 사용자 ID
     * @param documentId 문서 ID
     * @param expiresAt  만료 시각 (null이면 무기한)
     * @return 생성된 공유 링크
     * @throws BusinessException NOT_FOUND 문서가 없을 때, FORBIDDEN 다른 사용자의 문서일 때
     */
    ShareLinkResponse createShareLink(Long userId, Long documentId, java.time.LocalDateTime expiresAt);

    /**
     * (질의형) 문서의 현재 공유 링크를 조회한다. 없으면 빈 값을 반환한다.
     *
     * @param userId     로그인 사용자 ID
     * @param documentId 문서 ID
     * @return 현재 공유 링크 (없으면 empty)
     * @throws BusinessException NOT_FOUND 문서가 없을 때, FORBIDDEN 다른 사용자의 문서일 때
     */
    Optional<ShareLinkResponse> getShareLink(Long userId, Long documentId);

    /**
     * (명령형) 문서 공유 링크를 해제한다.
     *
     * @param userId     로그인 사용자 ID
     * @param documentId 문서 ID
     * @throws BusinessException NOT_FOUND 문서가 없을 때, FORBIDDEN 다른 사용자의 문서일 때
     */
    void revokeShareLink(Long userId, Long documentId);

    /**
     * (질의형) 공개 조회용으로 토큰이 유효한지 검증하고 소유자/문서를 반환한다.
     * 만료된 토큰이나 존재하지 않는 토큰은 빈 값을 반환한다.
     *
     * @param token 공유 토큰
     * @return 소유자 userId와 documentId (유효하지 않으면 empty)
     */
    Optional<ResolvedShare> resolve(String token);

    /**
     * (질의형) 공유 토큰으로 조립된 이력서 뷰 + 문서 메타데이터를 반환한다.
     * 토큰이 없거나 만료되면 BusinessException(NOT_FOUND)을 던진다.
     *
     * @param token 공유 토큰
     * @return 공개 이력서 뷰 (문서 템플릿/섹션 편성 포함)
     * @throws BusinessException NOT_FOUND 토큰이 없거나 만료되었을 때
     */
    ShareViewResponse getPublicView(String token);

    /**
     * 토큰 검증 결과. 공개 조회/PDF 생성에 사용한다.
     *
     * @param userId     소유자 ID
     * @param documentId 공유 대상 문서 ID
     */
    record ResolvedShare(Long userId, Long documentId) {
    }
}