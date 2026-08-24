package com.shplatform.resume.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import com.shplatform.resume.api.dto.DocumentCreateRequest;
import com.shplatform.resume.api.dto.DocumentUpdateRequest;
import com.shplatform.resume.infrastructure.entity.ResumeDocumentEntity;
import com.shplatform.resume.infrastructure.repository.ResumeDocumentRepository;
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
class ResumeDocumentServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long DOC_ID = 10L;

    @Mock
    private ResumeDocumentRepository documentRepository;

    @InjectMocks
    private ResumeDocumentServiceImpl documentService;

    private ResumeDocumentEntity entity(Long id, String title, boolean primary) {
        return ResumeDocumentEntity.create(USER_ID, title, "CLASSIC", primary,
                ResumeDocumentServiceImpl.DEFAULT_SECTION_CONFIG);
    }

    @Test
    @DisplayName("getDocuments: 문서가 없으면 기본 문서를 자동 생성한다")
    void getDocuments_empty_createsDefault() {
        given(documentRepository.findByUserIdOrderByCreatedAtAsc(USER_ID)).willReturn(List.of());
        given(documentRepository.save(any(ResumeDocumentEntity.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        var responses = documentService.getDocuments(USER_ID);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).title()).isEqualTo("내 이력서");
        assertThat(responses.get(0).primary()).isTrue();
        ArgumentCaptor<ResumeDocumentEntity> captor =
                ArgumentCaptor.forClass(ResumeDocumentEntity.class);
        then(documentRepository).should(times(1)).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("내 이력서");
    }

    @Test
    @DisplayName("getDocuments: 기존 문서가 있으면 그대로 반환한다")
    void getDocuments_existing() {
        var doc = entity(DOC_ID, "내 이력서", true);
        given(documentRepository.findByUserIdOrderByCreatedAtAsc(USER_ID)).willReturn(List.of(doc));

        var responses = documentService.getDocuments(USER_ID);

        then(documentRepository).should(times(0)).save(any());
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isNull();
        assertThat(responses.get(0).title()).isEqualTo("내 이력서");
    }

    @Test
    @DisplayName("createDocument: fromDocumentId로 기존 문서의 섹션편성을 복사한다")
    void createDocument_fromExisting() {
        var source = entity(99L, "원본", true);
        given(documentRepository.findByIdAndUserId(99L, USER_ID))
                .willReturn(Optional.of(source));
        given(documentRepository.save(any(ResumeDocumentEntity.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        var response = documentService.createDocument(USER_ID,
                new DocumentCreateRequest("사람인용", 99L));

        ArgumentCaptor<ResumeDocumentEntity> captor =
                ArgumentCaptor.forClass(ResumeDocumentEntity.class);
        then(documentRepository).should(times(1)).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("사람인용");
        assertThat(captor.getValue().isPrimary()).isFalse();
        assertThat(response.sectionConfig()).isEqualTo(ResumeDocumentServiceImpl.DEFAULT_SECTION_CONFIG);
    }

    @Test
    @DisplayName("createDocument: 불러올 문서가 없으면 NOT_FOUND 예외가 발생한다")
    void createDocument_sourceNotFound() {
        given(documentRepository.findByIdAndUserId(99L, USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.createDocument(USER_ID,
                new DocumentCreateRequest("신규", 99L)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("updateDocument: 제목과 섹션편성을 수정한다")
    void updateDocument_success() {
        var doc = entity(DOC_ID, "내 이력서", false);
        given(documentRepository.findByIdAndUserId(DOC_ID, USER_ID))
                .willReturn(Optional.of(doc));
        String newConfig = """
                [{"key":"careers","included":true,"order":1},
                 {"key":"skills","included":false,"order":2}]""";

        var response = documentService.updateDocument(USER_ID, DOC_ID,
                new DocumentUpdateRequest("수정된 제목", null, newConfig, null));

        assertThat(response.title()).isEqualTo("수정된 제목");
        assertThat(response.sectionConfig()).contains("\"included\":false");
    }

    @Test
    @DisplayName("updateDocument: 허용되지 않은 섹션 key면 INVALID_INPUT 예외가 발생한다")
    void updateDocument_invalidSectionKey() {
        var doc = entity(DOC_ID, "내 이력서", false);
        given(documentRepository.findByIdAndUserId(DOC_ID, USER_ID))
                .willReturn(Optional.of(doc));

        assertThatThrownBy(() -> documentService.updateDocument(USER_ID, DOC_ID,
                new DocumentUpdateRequest(null, null, "[{\"key\":\"hacked\",\"included\":true,\"order\":1}]", null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("updateDocument: JSON이 깨졌으면 INVALID_INPUT 예외가 발생한다")
    void updateDocument_brokenJson() {
        var doc = entity(DOC_ID, "내 이력서", false);
        given(documentRepository.findByIdAndUserId(DOC_ID, USER_ID))
                .willReturn(Optional.of(doc));

        assertThatThrownBy(() -> documentService.updateDocument(USER_ID, DOC_ID,
                new DocumentUpdateRequest(null, null, "{not-json", null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("markPrimary: 대상을 대표로 지정하고 나머지는 해제한다")
    void markPrimary_success() {
        var target = entity(DOC_ID, "대상", false);
        var other = entity(20L, "기존대표", true);
        given(documentRepository.findByIdAndUserId(DOC_ID, USER_ID))
                .willReturn(Optional.of(target));
        given(documentRepository.findByUserIdOrderByCreatedAtAsc(USER_ID))
                .willReturn(List.of(target, other));

        documentService.markPrimary(USER_ID, DOC_ID);

        assertThat(target.isPrimary()).isTrue();
        assertThat(other.isPrimary()).isFalse();
    }

    @Test
    @DisplayName("deleteDocument: 유일한 문서는 삭제할 수 없다")
    void deleteDocument_lastOneRejected() {
        var doc = entity(DOC_ID, "내 이력서", true);
        given(documentRepository.findByIdAndUserId(DOC_ID, USER_ID))
                .willReturn(Optional.of(doc));
        given(documentRepository.countByUserId(USER_ID)).willReturn(1L);

        assertThatThrownBy(() -> documentService.deleteDocument(USER_ID, DOC_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }
}
