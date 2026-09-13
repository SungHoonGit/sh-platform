package com.shplatform.resume.domain;

import com.shplatform.resume.infrastructure.entity.ResumePortfolioItemEntity;
import com.shplatform.resume.infrastructure.entity.ResumeProjectEntity;
import com.shplatform.resume.infrastructure.entity.SkillMasterEntity;
import com.shplatform.resume.infrastructure.repository.ResumePortfolioItemRepository;
import com.shplatform.resume.infrastructure.repository.ResumeProjectRepository;
import com.shplatform.resume.infrastructure.repository.SkillMasterRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class SkillMatchServiceImplTest {

    @Mock
    private SkillMasterRepository skillMasterRepository;

    @Mock
    private ResumeProjectRepository projectRepository;

    @Mock
    private ResumePortfolioItemRepository portfolioItemRepository;

    @InjectMocks
    private SkillMatchServiceImpl skillMatchService;

    private SkillMasterEntity master(String name, String aliases) {
        SkillMasterEntity e = mock(SkillMasterEntity.class);
        given(e.getName()).willReturn(name);
        given(e.getAliases()).willReturn(aliases);
        return e;
    }

    private ResumeProjectEntity project(String techStack) {
        ResumeProjectEntity e = ResumeProjectEntity.create(1L, 100L);
        e.setTechStack(techStack);
        return e;
    }

    private ResumePortfolioItemEntity portfolioItem(String techStack) {
        ResumePortfolioItemEntity e = ResumePortfolioItemEntity.create(1L, 100L);
        e.setTechStack(techStack);
        return e;
    }

    @Test
    @DisplayName("내 이력서 기술과 공고 기술을 마스터 기준으로 매칭한다")
    void match_basic() {
        SkillMasterEntity java = master("Java", "자바,JAVA");
        SkillMasterEntity react = master("React", "React.js,ReactJS");
        SkillMasterEntity spring = master("Spring", "스프링");
        given(skillMasterRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc())
                .willReturn(List.of(java, react, spring));
        given(projectRepository.findByUserIdOrderByDisplayOrderAscIdAsc(1L))
                .willReturn(List.of(project("Java, React")));
        given(portfolioItemRepository.findByUserIdOrderByDisplayOrderAscIdAsc(1L))
                .willReturn(List.of());

        var result = skillMatchService.match(1L, "Java, Vue.js");

        assertThat(result.matchedSkills()).containsExactly("Java");
        assertThat(result.matchCount()).isEqualTo(1);
        assertThat(result.requestedCount()).isEqualTo(2);
        assertThat(result.mySkillCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("별칭/유사어로 매칭한다")
    void match_alias() {
        SkillMasterEntity react = master("React", "React.js,ReactJS");
        SkillMasterEntity java = master("Java", "자바");
        given(skillMasterRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc())
                .willReturn(List.of(react, java));
        given(projectRepository.findByUserIdOrderByDisplayOrderAscIdAsc(1L))
                .willReturn(List.of(project("React")));
        given(portfolioItemRepository.findByUserIdOrderByDisplayOrderAscIdAsc(1L))
                .willReturn(List.of());

        var result = skillMatchService.match(1L, "React.js");

        assertThat(result.matchedSkills()).containsExactly("React");
        assertThat(result.matchCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("작업물의 기술 스택도 매칭에 포함한다")
    void match_includesPortfolioItems() {
        SkillMasterEntity docker = master("Docker", "도커");
        given(skillMasterRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc())
                .willReturn(List.of(docker));
        given(projectRepository.findByUserIdOrderByDisplayOrderAscIdAsc(1L))
                .willReturn(List.of());
        given(portfolioItemRepository.findByUserIdOrderByDisplayOrderAscIdAsc(1L))
                .willReturn(List.of(portfolioItem("Docker")));

        var result = skillMatchService.match(1L, "docker");

        assertThat(result.matchedSkills()).containsExactly("Docker");
        assertThat(result.mySkillCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("일치가 없으면 빈 목록을 반환한다")
    void match_noMatch() {
        SkillMasterEntity java = master("Java", "자바");
        given(skillMasterRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc())
                .willReturn(List.of(java));
        given(projectRepository.findByUserIdOrderByDisplayOrderAscIdAsc(1L))
                .willReturn(List.of(project("Kotlin")));
        given(portfolioItemRepository.findByUserIdOrderByDisplayOrderAscIdAsc(1L))
                .willReturn(List.of());

        var result = skillMatchService.match(1L, "Java");

        assertThat(result.matchedSkills()).isEmpty();
        assertThat(result.matchCount()).isZero();
    }

    @Test
    @DisplayName("공고 기술이 없으면 빈 결과를 반환한다")
    void match_emptyRequest() {
        SkillMasterEntity java = master("Java", "자바");
        given(skillMasterRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc())
                .willReturn(List.of(java));
        given(projectRepository.findByUserIdOrderByDisplayOrderAscIdAsc(1L))
                .willReturn(List.of(project("Java")));
        given(portfolioItemRepository.findByUserIdOrderByDisplayOrderAscIdAsc(1L))
                .willReturn(List.of());

        var result = skillMatchService.match(1L, "   ");

        assertThat(result.matchedSkills()).isEmpty();
        assertThat(result.matchCount()).isZero();
        assertThat(result.requestedCount()).isZero();
    }
}