package com.shplatform.resume.domain;

import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import com.shplatform.resume.api.dto.ProjectRequest;
import com.shplatform.resume.infrastructure.entity.ResumeProjectEntity;
import com.shplatform.resume.infrastructure.repository.ResumeProjectRepository;
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
class ProjectServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 999L;
    private static final Long PROJECT_ID = 500L;

    @Mock
    private ResumeProjectRepository projectRepository;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private ProjectRequest request() {
        return new ProjectRequest("sh-platform", "개발자",
                null, null, "통합 플랫폼", "Java, Spring Boot", "https://github.com", 1);
    }

    private ResumeProjectEntity entity(Long userId) {
        var e = ResumeProjectEntity.create(userId);
        e.setId(PROJECT_ID);
        e.setName("sh-platform");
        return e;
    }

    @Test
    @DisplayName("getProjects: 프로젝트 목록을 조회한다")
    void getProjects_success() {
        given(projectRepository.findByUserIdOrderByDisplayOrderAscIdAsc(USER_ID))
                .willReturn(List.of(entity(USER_ID)));

        var responses = projectService.getProjects(USER_ID);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).name()).isEqualTo("sh-platform");
    }

    @Test
    @DisplayName("createProject: 프로젝트를 추가한다")
    void createProject_success() {
        given(projectRepository.save(any(ResumeProjectEntity.class)))
                .willAnswer(invocation -> {
                    invocation.getArgument(0, ResumeProjectEntity.class).setId(PROJECT_ID);
                    return invocation.getArgument(0);
                });

        var response = projectService.createProject(USER_ID, request());

        ArgumentCaptor<ResumeProjectEntity> captor = ArgumentCaptor.forClass(ResumeProjectEntity.class);
        then(projectRepository).should(times(1)).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(response.id()).isEqualTo(PROJECT_ID);
    }

    @Test
    @DisplayName("updateProject: 내 프로젝트를 수정한다")
    void updateProject_success() {
        var existing = entity(USER_ID);
        given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(existing));
        given(projectRepository.save(any(ResumeProjectEntity.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        var response = projectService.updateProject(USER_ID, PROJECT_ID, request());

        then(projectRepository).should(times(1)).save(existing);
        assertThat(response.name()).isEqualTo("sh-platform");
    }

    @Test
    @DisplayName("updateProject: 다른 사용자의 프로젝트이면 FORBIDDEN 예외가 발생한다")
    void updateProject_forbidden() {
        given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(entity(OTHER_USER_ID)));

        assertThatThrownBy(() -> projectService.updateProject(USER_ID, PROJECT_ID, request()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("deleteProject: 내 프로젝트를 삭제한다")
    void deleteProject_success() {
        var existing = entity(USER_ID);
        given(projectRepository.findById(PROJECT_ID)).willReturn(Optional.of(existing));

        projectService.deleteProject(USER_ID, PROJECT_ID);

        then(projectRepository).should(times(1)).delete(existing);
    }
}
