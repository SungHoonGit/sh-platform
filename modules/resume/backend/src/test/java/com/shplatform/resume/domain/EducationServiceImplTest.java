package com.shplatform.resume.domain;

import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import com.shplatform.resume.api.dto.EducationRequest;
import com.shplatform.resume.infrastructure.entity.ResumeEducationEntity;
import com.shplatform.resume.infrastructure.repository.ResumeEducationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class EducationServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 999L;
    private static final Long EDUCATION_ID = 200L;
    private static final Long DOCUMENT_ID = 100L;

    @Mock
    private ResumeEducationRepository educationRepository;

    @InjectMocks
    private EducationServiceImpl educationService;

    private EducationRequest request() {
        return new EducationRequest("한국대학교", "대학교", "컴퓨터공학", "BACHELOR", "3.9 / 4.5",
                null, null, "GRADUATED", 1);
    }

    private ResumeEducationEntity entity(Long userId) {
        var e = ResumeEducationEntity.create(userId, DOCUMENT_ID);
        e.setId(EDUCATION_ID);
        e.setSchool("한국대학교");
        return e;
    }

    @Test
    @DisplayName("getEducations: 학력 목록을 조회한다")
    void getEducations_success() {
        given(educationRepository.findByUserIdAndDocumentIdOrderByDisplayOrderAscIdAsc(USER_ID, DOCUMENT_ID))
                .willReturn(List.of(entity(USER_ID)));

        var responses = educationService.getEducations(USER_ID, DOCUMENT_ID);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).school()).isEqualTo("한국대학교");
    }

    @Test
    @DisplayName("createEducation: 학력을 추가한다")
    void createEducation_success() {
        given(educationRepository.save(any(ResumeEducationEntity.class)))
                .willAnswer(invocation -> {
                    invocation.getArgument(0, ResumeEducationEntity.class).setId(EDUCATION_ID);
                    return invocation.getArgument(0);
                });

        var response = educationService.createEducation(USER_ID, DOCUMENT_ID, request());

        ArgumentCaptor<ResumeEducationEntity> captor = ArgumentCaptor.forClass(ResumeEducationEntity.class);
        then(educationRepository).should(times(1)).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(response.id()).isEqualTo(EDUCATION_ID);
    }

    @Test
    @DisplayName("updateEducation: 내 학력을 수정한다")
    void updateEducation_success() {
        var existing = entity(USER_ID);
        given(educationRepository.findById(EDUCATION_ID)).willReturn(Optional.of(existing));
        given(educationRepository.save(any(ResumeEducationEntity.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        var response = educationService.updateEducation(USER_ID, DOCUMENT_ID, EDUCATION_ID, request());

        then(educationRepository).should(times(1)).save(existing);
        assertThat(response.school()).isEqualTo("한국대학교");
    }

    @Test
    @DisplayName("updateEducation: 학력이 없으면 NOT_FOUND 예외가 발생한다")
    void updateEducation_notFound() {
        given(educationRepository.findById(EDUCATION_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> educationService.updateEducation(USER_ID, DOCUMENT_ID, EDUCATION_ID, request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("updateEducation: 다른 사용자의 학력이면 FORBIDDEN 예외가 발생한다")
    void updateEducation_forbidden() {
        given(educationRepository.findById(EDUCATION_ID)).willReturn(Optional.of(entity(OTHER_USER_ID)));

        assertThatThrownBy(() -> educationService.updateEducation(USER_ID, DOCUMENT_ID, EDUCATION_ID, request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("deleteEducation: 내 학력을 삭제한다")
    void deleteEducation_success() {
        var existing = entity(USER_ID);
        given(educationRepository.findById(EDUCATION_ID)).willReturn(Optional.of(existing));

        educationService.deleteEducation(USER_ID, DOCUMENT_ID, EDUCATION_ID);

        then(educationRepository).should(times(1)).delete(existing);
    }

    @Test
    @DisplayName("reorderEducations: 전달된 id 순서대로 displayOrder를 재지정한다")
    void reorderEducations_success() {
        var first = entity(USER_ID);
        first.setId(201L);
        first.setDisplayOrder(1);
        var second = entity(USER_ID);
        second.setId(202L);
        second.setDisplayOrder(2);
        given(educationRepository.findByUserIdAndDocumentIdOrderByDisplayOrderAscIdAsc(USER_ID, DOCUMENT_ID))
                .willReturn(List.of(first, second));
        given(educationRepository.saveAll(any()))
                .willAnswer(invocation -> invocation.getArgument(0));

        educationService.reorderEducations(USER_ID, DOCUMENT_ID, List.of(202L, 201L));

        assertThat(first.getDisplayOrder()).isEqualTo(2);
        assertThat(second.getDisplayOrder()).isEqualTo(1);
        then(educationRepository).should(times(1)).saveAll(List.of(first, second));
    }

    @Test
    @DisplayName("reorderEducations: 본인 소유가 아닌 학력 id가 포함되면 FORBIDDEN 예외가 발생한다")
    void reorderEducations_forbidden() {
        given(educationRepository.findByUserIdAndDocumentIdOrderByDisplayOrderAscIdAsc(USER_ID, DOCUMENT_ID))
                .willReturn(List.of(entity(USER_ID)));

        assertThatThrownBy(() -> educationService.reorderEducations(USER_ID, DOCUMENT_ID, List.of(999L)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }
}
