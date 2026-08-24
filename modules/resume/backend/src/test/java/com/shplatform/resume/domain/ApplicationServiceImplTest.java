package com.shplatform.resume.domain;

import com.shplatform.common.exception.BusinessException;
import com.shplatform.resume.api.dto.ApplicationRequest;
import com.shplatform.resume.api.dto.ApplicationResponse;
import com.shplatform.resume.infrastructure.ApplicationEntity;
import com.shplatform.resume.infrastructure.ApplicationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceImplTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @InjectMocks
    private ApplicationServiceImpl applicationService;

    private final Long userId = 6L;

    @Test
    @DisplayName("지원을 등록하면 기본값(LINK/PREPARING)이 적용된다")
    void create_appliesDefaults() {
        given(applicationRepository.save(any())).willAnswer(inv -> {
            ApplicationEntity e = inv.getArgument(0);
            return ApplicationEntity.builder()
                    .id(1L).userId(e.getUserId())
                    .companyName(e.getCompanyName()).postingTitle(e.getPostingTitle())
                    .applyChannel(e.getApplyChannel()).status(e.getStatus())
                    .appliedAt(e.getAppliedAt()).build();
        });
        ApplicationRequest request = new ApplicationRequest(
                "네이버", "백엔드 개발자", "https://recruit.naver.com/1",
                null, LocalDate.of(2026, 8, 24), null, null, null, null);

        applicationService.create(userId, request);

        ArgumentCaptor<ApplicationEntity> captor = ArgumentCaptor.forClass(ApplicationEntity.class);
        then(applicationRepository).should().save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getApplyChannel()).isEqualTo("LINK");
        assertThat(captor.getValue().getStatus()).isEqualTo("PREPARING");
        assertThat(captor.getValue().getCompanyName()).isEqualTo("네이버");
    }

    @Test
    @DisplayName("유효하지 않은 상태 코드는 PREPARING으로 저장된다")
    void create_sanitizesInvalidStatus() {
        given(applicationRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        ApplicationRequest request = new ApplicationRequest(
                "삼성전자", "프론트엔드", null, "EMAIL", null, "HACKED", null, null, null);

        applicationService.create(userId, request);

        ArgumentCaptor<ApplicationEntity> captor = ArgumentCaptor.forClass(ApplicationEntity.class);
        then(applicationRepository).should().save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("PREPARING");
        assertThat(captor.getValue().getApplyChannel()).isEqualTo("EMAIL");
    }

    @Test
    @DisplayName("내 지원 목록을 전체 조회한다")
    void getApplications_returnsAll() {
        given(applicationRepository.findByUserIdOrderByAppliedAtDescIdDesc(userId))
                .willReturn(List.of(sampleEntity(1L, "APPLIED")));

        List<ApplicationResponse> result = applicationService.getApplications(userId, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).companyName()).isEqualTo("네이버");
    }

    @Test
    @DisplayName("상태 필터로 목록을 조회한다")
    void getApplications_filtersByStatus() {
        given(applicationRepository.findByUserIdAndStatusOrderByAppliedAtDescIdDesc(userId, "INTERVIEW"))
                .willReturn(List.of(sampleEntity(2L, "INTERVIEW")));

        List<ApplicationResponse> result = applicationService.getApplications(userId, "INTERVIEW");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo("INTERVIEW");
    }

    @Test
    @DisplayName("지원을 수정한다")
    void update_modifiesFields() {
        ApplicationEntity entity = sampleEntity(1L, "PREPARING");
        given(applicationRepository.findById(1L)).willReturn(Optional.of(entity));
        ApplicationRequest request = new ApplicationRequest(
                "카카오", "백엔드", null, "PLATFORM", LocalDate.of(2026, 8, 20),
                "SCREEN_PASSED", 3L, null, "서류 통과!");

        ApplicationResponse response = applicationService.update(userId, 1L, request);

        assertThat(response.status()).isEqualTo("SCREEN_PASSED");
        assertThat(response.companyName()).isEqualTo("카카오");
        assertThat(entity.getStatus()).isEqualTo("SCREEN_PASSED");
    }

    @Test
    @DisplayName("다른 사용자의 지원은 수정할 수 없다")
    void update_throwsWhenNotOwner() {
        ApplicationEntity entity = sampleEntity(1L, "APPLIED");
        entity.setUserId(999L);
        given(applicationRepository.findById(1L)).willReturn(Optional.of(entity));

        assertThatThrownBy(() -> applicationService.update(userId, 1L,
                new ApplicationRequest("x", "x", null, null, null, null, null, null, null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("존재하지 않는 지원 삭제 시 예외가 발생한다")
    void delete_throwsWhenMissing() {
        given(applicationRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.delete(userId, 99L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("본인의 지원을 삭제한다")
    void delete_removesOwned() {
        ApplicationEntity entity = sampleEntity(1L, "APPLIED");
        given(applicationRepository.findById(1L)).willReturn(Optional.of(entity));

        applicationService.delete(userId, 1L);

        then(applicationRepository).should().delete(entity);
    }

    @Test
    @DisplayName("다른 사용자의 지원은 삭제할 수 없다")
    void delete_throwsWhenNotOwner() {
        ApplicationEntity entity = sampleEntity(1L, "APPLIED");
        entity.setUserId(999L);
        given(applicationRepository.findById(1L)).willReturn(Optional.of(entity));

        assertThatThrownBy(() -> applicationService.delete(userId, 1L))
                .isInstanceOf(BusinessException.class);
        then(applicationRepository).should(never()).delete(any());
    }

    private ApplicationEntity sampleEntity(Long id, String status) {
        return ApplicationEntity.builder()
                .id(id)
                .userId(userId)
                .companyName("네이버")
                .postingTitle("백엔드 개발자")
                .postingUrl("https://recruit.naver.com/1")
                .applyChannel("LINK")
                .appliedAt(LocalDate.of(2026, 8, 20))
                .status(status)
                .build();
    }
}
