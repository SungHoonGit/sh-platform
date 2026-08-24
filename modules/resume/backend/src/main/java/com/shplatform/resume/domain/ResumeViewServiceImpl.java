package com.shplatform.resume.domain;

import com.shplatform.common.exception.BusinessException;
import com.shplatform.resume.api.dto.ProfileResponse;
import com.shplatform.resume.api.dto.ResumeViewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumeViewServiceImpl implements ResumeViewService {

    private final ResumeProfileService resumeProfileService;
    private final CareerService careerService;
    private final EducationService educationService;
    private final SkillService skillService;
    private final CertificateService certificateService;
    private final ProjectService projectService;
    private final IntroductionService introductionService;
    private final PortfolioItemService portfolioItemService;

    @Override
    public ResumeViewResponse getMyResumeView(Long userId) {
        return new ResumeViewResponse(
                findProfileOrNull(userId),
                careerService.getCareers(userId),
                educationService.getEducations(userId),
                skillService.getSkills(userId),
                certificateService.getCertificates(userId),
                projectService.getProjects(userId),
                introductionService.getIntroductions(userId),
                portfolioItemService.getPortfolioItems(userId),
                java.time.LocalDateTime.now()
        );
    }

    private ProfileResponse findProfileOrNull(Long userId) {
        try {
            return resumeProfileService.getMyProfile(userId);
        } catch (BusinessException e) {
            return null;
        }
    }
}
