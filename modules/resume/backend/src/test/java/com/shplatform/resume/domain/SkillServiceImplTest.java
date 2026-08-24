package com.shplatform.resume.domain;

import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import com.shplatform.resume.api.dto.SkillRequest;
import com.shplatform.resume.infrastructure.entity.ResumeSkillEntity;
import com.shplatform.resume.infrastructure.repository.ResumeSkillRepository;
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
class SkillServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 999L;
    private static final Long SKILL_ID = 300L;

    @Mock
    private ResumeSkillRepository skillRepository;

    @InjectMocks
    private SkillServiceImpl skillService;

    private SkillRequest request() {
        return new SkillRequest("Java", "ADVANCED", "LANGUAGE", 1);
    }

    private ResumeSkillEntity entity(Long userId) {
        var e = ResumeSkillEntity.create(userId);
        e.setId(SKILL_ID);
        e.setName("Java");
        return e;
    }

    @Test
    @DisplayName("getSkills: 스킬 목록을 조회한다")
    void getSkills_success() {
        given(skillRepository.findByUserIdOrderByDisplayOrderAscIdAsc(USER_ID))
                .willReturn(List.of(entity(USER_ID)));

        var responses = skillService.getSkills(USER_ID);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).name()).isEqualTo("Java");
    }

    @Test
    @DisplayName("createSkill: 스킬을 추가한다")
    void createSkill_success() {
        given(skillRepository.save(any(ResumeSkillEntity.class)))
                .willAnswer(invocation -> {
                    invocation.getArgument(0, ResumeSkillEntity.class).setId(SKILL_ID);
                    return invocation.getArgument(0);
                });

        var response = skillService.createSkill(USER_ID, request());

        ArgumentCaptor<ResumeSkillEntity> captor = ArgumentCaptor.forClass(ResumeSkillEntity.class);
        then(skillRepository).should(times(1)).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(response.id()).isEqualTo(SKILL_ID);
    }

    @Test
    @DisplayName("updateSkill: 내 스킬을 수정한다")
    void updateSkill_success() {
        var existing = entity(USER_ID);
        given(skillRepository.findById(SKILL_ID)).willReturn(Optional.of(existing));
        given(skillRepository.save(any(ResumeSkillEntity.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        var response = skillService.updateSkill(USER_ID, SKILL_ID, request());

        then(skillRepository).should(times(1)).save(existing);
        assertThat(response.name()).isEqualTo("Java");
    }

    @Test
    @DisplayName("updateSkill: 다른 사용자의 스킬이면 FORBIDDEN 예외가 발생한다")
    void updateSkill_forbidden() {
        given(skillRepository.findById(SKILL_ID)).willReturn(Optional.of(entity(OTHER_USER_ID)));

        assertThatThrownBy(() -> skillService.updateSkill(USER_ID, SKILL_ID, request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("deleteSkill: 내 스킬을 삭제한다")
    void deleteSkill_success() {
        var existing = entity(USER_ID);
        given(skillRepository.findById(SKILL_ID)).willReturn(Optional.of(existing));

        skillService.deleteSkill(USER_ID, SKILL_ID);

        then(skillRepository).should(times(1)).delete(existing);
    }
}
