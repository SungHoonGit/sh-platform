package com.shplatform.resume.domain;

import com.shplatform.resume.api.dto.DocumentCreateRequest;
import com.shplatform.resume.api.dto.DocumentResponse;
import com.shplatform.resume.api.dto.DocumentUpdateRequest;
import java.util.List;

/**
 * 이력서 문서(뷰 정의) 도메인 서비스.
 */
public interface ResumeDocumentService {

    /**
     * (질의형) 내 문서 목록을 조회한다. 등록된 문서가 없으면 기본 문서("내 이력서")를
     * 자동 생성한 뒤 반환한다 — 기존 사용자 마이그레이션 역할을 겸한다.
     *
     * @param userId 로그인 사용자 ID
     * @return 문서 목록 (생성순)
     */
    List<DocumentResponse> getDocuments(Long userId);

    /**
     * (명령형) 문서를 생성한다. fromDocumentId가 지정되면 해당 문서의 섹션 편성을
     * 복사해 "불러오기"로 동작하고, 아니면 기본 편성(전 섹션 포함)으로 생성한다.
     *
     * @param userId  로그인 사용자 ID
     * @param request 제목, 불러올 문서 ID(선택)
     * @return 생성된 문서
     * @throws BusinessException NOT_FOUND 불러올 문서가 없을 때,
     *                          FORBIDDEN 다른 사용자의 문서일 때
     */
    DocumentResponse createDocument(Long userId, DocumentCreateRequest request);

    /**
     * (명령형) 문서를 수정한다. null 필드는 변경하지 않는다.
     *
     * @param userId     로그인 사용자 ID
     * @param documentId 문서 ID
     * @param request    제목/템플릿/섹션편성/대표여부 (전부 선택)
     * @return 수정된 문서
     * @throws BusinessException NOT_FOUND 문서가 없을 때, FORBIDDEN 다른 사용자의 문서일 때,
     *                          INVALID_INPUT sectionConfig가 올바른 형식이 아닐 때
     */
    DocumentResponse updateDocument(Long userId, Long documentId, DocumentUpdateRequest request);

    /**
     * (명령형) 대표 문서로 지정한다. 기존 대표 문서는 해제된다.
     *
     * @param userId     로그인 사용자 ID
     * @param documentId 문서 ID
     * @throws BusinessException NOT_FOUND 문서가 없을 때, FORBIDDEN 다른 사용자의 문서일 때
     */
    void markPrimary(Long userId, Long documentId);

    /**
     * (명령형) 문서를 삭제한다. 마지막 남은 문서는 삭제할 수 없다.
     *
     * @param userId     로그인 사용자 ID
     * @param documentId 문서 ID
     * @throws BusinessException NOT_FOUND 문서가 없을 때, FORBIDDEN 다른 사용자의 문서일 때,
     *                          INVALID_INPUT 유일한 문서를 삭제하려 할 때
     */
    void deleteDocument(Long userId, Long documentId);
}
