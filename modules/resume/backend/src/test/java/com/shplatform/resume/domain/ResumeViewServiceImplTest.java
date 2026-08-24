package com.shplatform.resume.domain;

import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import com.shplatform.resume.api.dto.ProfileResponse;
import com.shplatform.resume.api.dto.ResumeViewResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ResumeViewServiceImplTest {

    private static final Long USER_ID = 1L;

    @Mock
    private ResumeProfileService resumeProfileService;
    @Mock
    private CareerService careerService;
    @Mock
    private EducationService educationService;
    @Mock
    private SkillService skillService;
    @Mock
    private CertificateService certificateService;
    @Mock
    private ProjectService projectService;
    @Mock
    private IntroductionService introductionService;
    @Mock
    private PortfolioItemService portfolioItemService;

    @InjectMocks
    private ResumeViewServiceImpl resumeViewService;

    @Test
    @DisplayName("getMyResumeView: 8개 항목을 모두 조립한다")
    void getMyResumeView_assemblesAll() {
        given(resumeProfileService.getMyProfile(USER_ID))
                .willReturn(new ProfileResponse(10L, "홍길동", null, null, null, null, null, "소개", null, null));
        given(careerService.getCareers(USER_ID)).willReturn(List.of());
        given(educationService.getEducations(USER_ID)).willReturn(List.of());
        given(skillService.getSkills(USER_ID)).willReturn(List.of());
        given(certificateService.getCertificates(USER_ID)).willReturn(List.of());
        given(projectService.getProjects(USER_ID)).willReturn(List.of());
        given(introductionService.getIntroductions(USER_ID)).willReturn(List.of());
        given(portfolioItemService.getPortfolioItems(USER_ID)).willReturn(List.of());

        ResumeViewResponse response = resumeViewService.getMyResumeView(USER_ID);

        assertThat(response.profile().name()).isEqualTo("홍길동");
        assertThat(response.careers()).isEmpty();
        assertThat(response.generatedAt()).isNotNull();
    }

    @Test
    @DisplayName("getMyResumeView: 인적사항이 미등록이어도 profile만 null로 정상 반환된다")
    void getMyResumeView_profileMissing() {
        given(resumeProfileService.getMyProfile(USER_ID))
                .willThrow(new BusinessException(ErrorCode.NOT_FOUND));
        given(careerService.getCareers(USER_ID)).willReturn(List.of());
        given(educationService.getEducations(USER_ID)).willReturn(List.of());
        given(skillService.getSkills(USER_ID)).willReturn(List.of());
        given(certificateService.getCertificates(USER_ID)).willReturn(List.of());
        given(projectService.getProjects(USER_ID)).willReturn(List.of());
        given(introductionService.getIntroductions(USER_ID)).willReturn(List.of());
        given(portfolioItemService.getPortfolioItems(USER_ID)).willReturn(List.of());

        ResumeViewResponse response = resumeViewService.getMyResumeView(USER_ID);

        assertThat(response.profile()).isNull();
        assertThat(response.skills()).isEmpty();
    }
}
