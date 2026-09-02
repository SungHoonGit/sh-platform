package com.shplatform.resume.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import com.shplatform.resume.api.dto.DocumentResponse;
import com.shplatform.resume.api.dto.ResumeViewResponse;
import com.shplatform.resume.infrastructure.entity.ResumeDocumentEntity;
import com.shplatform.resume.infrastructure.entity.ResumeShareLinkEntity;
import com.shplatform.resume.infrastructure.repository.ResumeDocumentRepository;
import com.shplatform.resume.infrastructure.repository.ResumeShareLinkRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResumeShareServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long DOC_ID = 10L;
    private static final String TOKEN = "abc123token";

    @Mock
    private ResumeDocumentRepository documentRepository;
    @Mock
    private ResumeShareLinkRepository shareLinkRepository;
    @Mock
    private ResumeViewService resumeViewService;
    @Mock
    private ResumeDocumentService resumeDocumentService;

    @InjectMocks
    private ResumeShareServiceImpl shareService;

    private ResumeShareLinkEntity link(Long documentId, LocalDateTime expiresAt) {
        return ResumeShareLinkEntity.create(documentId, TOKEN, expiresAt);
    }

    @Test
    @DisplayName("createShareLink: 문서 소유자가 아니면 NOT_FOUND 예외가 발생한다")
    void createShareLink_notOwner() {
        given(documentRepository.findByIdAndUserId(DOC_ID, USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> shareService.createShareLink(USER_ID, DOC_ID, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("createShareLink: 기존 링크를 삭제하고 새 토큰으로 재발급한다")
    void createShareLink_regenerate() {
        var owner = ResumeDocumentEntity.create(USER_ID, "내 이력서", "CLASSIC", true, "[]");
        given(documentRepository.findByIdAndUserId(DOC_ID, USER_ID))
                .willReturn(Optional.of(owner));
        given(shareLinkRepository.save(any(ResumeShareLinkEntity.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        var response = shareService.createShareLink(USER_ID, DOC_ID, null);

        then(shareLinkRepository).should(times(1)).deleteByDocumentId(DOC_ID);
        ArgumentCaptor<ResumeShareLinkEntity> captor =
                ArgumentCaptor.forClass(ResumeShareLinkEntity.class);
        then(shareLinkRepository).should(times(1)).save(captor.capture());
        assertThat(captor.getValue().getDocumentId()).isEqualTo(DOC_ID);
        assertThat(captor.getValue().getToken()).hasSize(32);
        assertThat(captor.getValue().getExpiresAt()).isNull();
        assertThat(response.documentId()).isEqualTo(DOC_ID);
        assertThat(response.token()).hasSize(32);
    }

    @Test
    @DisplayName("getShareLink: 문서 소유자가 아니면 NOT_FOUND 예외가 발생한다")
    void getShareLink_notOwner() {
        given(documentRepository.findByIdAndUserId(DOC_ID, USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> shareService.getShareLink(USER_ID, DOC_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("getShareLink: 공유 링크가 없으면 빈 값을 반환한다")
    void getShareLink_empty() {
        var owner = ResumeDocumentEntity.create(USER_ID, "내 이력서", "CLASSIC", true, "[]");
        given(documentRepository.findByIdAndUserId(DOC_ID, USER_ID))
                .willReturn(Optional.of(owner));
        given(shareLinkRepository.findByDocumentId(DOC_ID)).willReturn(Optional.empty());

        var result = shareService.getShareLink(USER_ID, DOC_ID);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getShareLink: 존재하는 공유 링크를 반환한다")
    void getShareLink_found() {
        var owner = ResumeDocumentEntity.create(USER_ID, "내 이력서", "CLASSIC", true, "[]");
        var saved = link(DOC_ID, null);
        given(documentRepository.findByIdAndUserId(DOC_ID, USER_ID))
                .willReturn(Optional.of(owner));
        given(shareLinkRepository.findByDocumentId(DOC_ID)).willReturn(Optional.of(saved));

        var result = shareService.getShareLink(USER_ID, DOC_ID);

        assertThat(result).isPresent();
        assertThat(result.get().token()).isEqualTo(TOKEN);
        assertThat(result.get().documentId()).isEqualTo(DOC_ID);
    }

    @Test
    @DisplayName("revokeShareLink: 문서 소유자가 아니면 NOT_FOUND 예외가 발생한다")
    void revokeShareLink_notOwner() {
        given(documentRepository.findByIdAndUserId(DOC_ID, USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> shareService.revokeShareLink(USER_ID, DOC_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("revokeShareLink: 소유자이면 링크를 삭제한다")
    void revokeShareLink_success() {
        var owner = ResumeDocumentEntity.create(USER_ID, "내 이력서", "CLASSIC", true, "[]");
        given(documentRepository.findByIdAndUserId(DOC_ID, USER_ID))
                .willReturn(Optional.of(owner));

        shareService.revokeShareLink(USER_ID, DOC_ID);

        then(shareLinkRepository).should(times(1)).deleteByDocumentId(DOC_ID);
    }

    @Test
    @DisplayName("resolve: 존재하지 않는 토큰이면 빈 값을 반환한다")
    void resolve_unknownToken() {
        given(shareLinkRepository.findByToken("no-such-token")).willReturn(Optional.empty());

        assertThat(shareService.resolve("no-such-token")).isEmpty();
    }

    @Test
    @DisplayName("resolve: 만료된 토큰이면 빈 값을 반환한다")
    void resolve_expiredToken() {
        given(shareLinkRepository.findByToken(TOKEN))
                .willReturn(Optional.of(link(DOC_ID, LocalDateTime.now().minusMinutes(1))));

        assertThat(shareService.resolve(TOKEN)).isEmpty();
    }

    @Test
    @DisplayName("resolve: 유효한 토큰이면 소유자와 문서를 반환한다")
    void resolve_validToken() {
        var owner = ResumeDocumentEntity.create(USER_ID, "내 이력서", "CLASSIC", true, "[]");
        var saved = link(DOC_ID, null);
        given(shareLinkRepository.findByToken(TOKEN)).willReturn(Optional.of(saved));
        given(documentRepository.findById(DOC_ID)).willReturn(Optional.of(owner));

        var result = shareService.resolve(TOKEN);

        assertThat(result).isPresent();
        assertThat(result.get().userId()).isEqualTo(USER_ID);
        assertThat(result.get().documentId()).isEqualTo(DOC_ID);
    }

    @Test
    @DisplayName("getPublicView: 토큰이 유효하지 않으면 NOT_FOUND 예외가 발생한다")
    void getPublicView_invalidToken() {
        given(shareLinkRepository.findByToken("bad")).willReturn(Optional.empty());

        assertThatThrownBy(() -> shareService.getPublicView("bad"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("getPublicView: 조립된 뷰와 문서 메타데이터를 함께 반환한다")
    void getPublicView_success() {
        var owner = ResumeDocumentEntity.create(USER_ID, "내 이력서", "CLASSIC", true, "[]");
        var saved = link(DOC_ID, null);
        var docResponse = new DocumentResponse(
                DOC_ID, "내 이력서", "CLASSIC", true, "[]", null, null);
        var view = new ResumeViewResponse(null, null, null, null, null, null, null, null, null);
        given(shareLinkRepository.findByToken(TOKEN)).willReturn(Optional.of(saved));
        given(documentRepository.findById(DOC_ID)).willReturn(Optional.of(owner));
        given(resumeDocumentService.getDocuments(USER_ID)).willReturn(List.of(docResponse));
        given(resumeViewService.getMyResumeView(USER_ID)).willReturn(view);

        var response = shareService.getPublicView(TOKEN);

        assertThat(response.documentId()).isEqualTo(DOC_ID);
        assertThat(response.title()).isEqualTo("내 이력서");
        assertThat(response.templateCode()).isEqualTo("CLASSIC");
        assertThat(response.view()).isSameAs(view);
    }
}