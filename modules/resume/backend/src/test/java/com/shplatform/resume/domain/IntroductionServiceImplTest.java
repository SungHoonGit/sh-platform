package com.shplatform.resume.domain;

import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import com.shplatform.resume.api.dto.IntroductionRequest;
import com.shplatform.resume.infrastructure.entity.ResumeIntroductionEntity;
import com.shplatform.resume.infrastructure.repository.ResumeIntroductionRepository;
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
class IntroductionServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 999L;
    private static final Long INTRODUCTION_ID = 600L;

    @Mock
    private ResumeIntroductionRepository introductionRepository;

    @InjectMocks
    private IntroductionServiceImpl introductionService;

    private IntroductionRequest request() {
        return new IntroductionRequest("지원동기", "백엔드 개발자로 성장하고 싶습니다.", 1);
    }

    private ResumeIntroductionEntity entity(Long userId) {
        var e = ResumeIntroductionEntity.create(userId);
        e.setId(INTRODUCTION_ID);
        e.setTitle("지원동기");
        e.setContent("내용");
        return e;
    }

    @Test
    @DisplayName("getIntroductions: 자기소개 항목 목록을 조회한다")
    void getIntroductions_success() {
        given(introductionRepository.findByUserIdOrderByDisplayOrderAscIdAsc(USER_ID))
                .willReturn(List.of(entity(USER_ID)));

        var responses = introductionService.getIntroductions(USER_ID);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).title()).isEqualTo("지원동기");
    }

    @Test
    @DisplayName("createIntroduction: 자기소개 항목을 추가한다")
    void createIntroduction_success() {
        given(introductionRepository.save(any(ResumeIntroductionEntity.class)))
                .willAnswer(invocation -> {
                    invocation.getArgument(0, ResumeIntroductionEntity.class).setId(INTRODUCTION_ID);
                    return invocation.getArgument(0);
                });

        var response = introductionService.createIntroduction(USER_ID, request());

        ArgumentCaptor<ResumeIntroductionEntity> captor = ArgumentCaptor.forClass(ResumeIntroductionEntity.class);
        then(introductionRepository).should(times(1)).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(response.id()).isEqualTo(INTRODUCTION_ID);
    }

    @Test
    @DisplayName("updateIntroduction: 내 자기소개 항목을 수정한다")
    void updateIntroduction_success() {
        var existing = entity(USER_ID);
        given(introductionRepository.findById(INTRODUCTION_ID)).willReturn(Optional.of(existing));
        given(introductionRepository.save(any(ResumeIntroductionEntity.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        var response = introductionService.updateIntroduction(USER_ID, INTRODUCTION_ID, request());

        then(introductionRepository).should(times(1)).save(existing);
        assertThat(response.title()).isEqualTo("지원동기");
    }

    @Test
    @DisplayName("updateIntroduction: 다른 사용자의 항목이면 FORBIDDEN 예외가 발생한다")
    void updateIntroduction_forbidden() {
        given(introductionRepository.findById(INTRODUCTION_ID)).willReturn(Optional.of(entity(OTHER_USER_ID)));

        assertThatThrownBy(() -> introductionService.updateIntroduction(USER_ID, INTRODUCTION_ID, request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("deleteIntroduction: 내 자기소개 항목을 삭제한다")
    void deleteIntroduction_success() {
        var existing = entity(USER_ID);
        given(introductionRepository.findById(INTRODUCTION_ID)).willReturn(Optional.of(existing));

        introductionService.deleteIntroduction(USER_ID, INTRODUCTION_ID);

        then(introductionRepository).should(times(1)).delete(existing);
    }

    @Test
    @DisplayName("reorderIntroductions: 전달된 id 순서대로 displayOrder를 재지정한다")
    void reorderIntroductions_success() {
        var first = entity(USER_ID);
        first.setId(601L);
        first.setDisplayOrder(1);
        var second = entity(USER_ID);
        second.setId(602L);
        second.setDisplayOrder(2);
        given(introductionRepository.findByUserIdOrderByDisplayOrderAscIdAsc(USER_ID))
                .willReturn(List.of(first, second));
        given(introductionRepository.saveAll(any()))
                .willAnswer(invocation -> invocation.getArgument(0));

        introductionService.reorderIntroductions(USER_ID, List.of(602L, 601L));

        assertThat(first.getDisplayOrder()).isEqualTo(2);
        assertThat(second.getDisplayOrder()).isEqualTo(1);
        then(introductionRepository).should(times(1)).saveAll(List.of(first, second));
    }

    @Test
    @DisplayName("reorderIntroductions: 본인 소유가 아닌 항목 id가 포함되면 FORBIDDEN 예외가 발생한다")
    void reorderIntroductions_forbidden() {
        given(introductionRepository.findByUserIdOrderByDisplayOrderAscIdAsc(USER_ID))
                .willReturn(List.of(entity(USER_ID)));

        assertThatThrownBy(() -> introductionService.reorderIntroductions(USER_ID, List.of(999L)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }
}
