package com.shplatform.resume.domain;

import com.shplatform.common.exception.BusinessException;
import com.shplatform.resume.api.dto.ProfileResponse;
import com.shplatform.resume.api.dto.ResumeViewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// 의도적으로 @Transactional 미적용:
// 외부 트랜잭션으로 감싸면 내부 서비스(NOT_FOUND)가 공유 트랜잭션을
// rollback-only로 표시해 UnexpectedRollbackException 발생함.
// 각 항목 조회는 독립 트랜잭션으로 수행하고 예외는 여기서 흡수한다.
@Service
@RequiredArgsConstructor
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
