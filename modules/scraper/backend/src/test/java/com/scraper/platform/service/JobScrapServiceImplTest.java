package com.scraper.platform.service;

import com.scraper.platform.api.dto.JobScrapResponse;
import com.scraper.platform.model.JobPosting;
import com.scraper.platform.model.JobScrap;
import com.scraper.platform.repository.JobPostingRepository;
import com.scraper.platform.repository.JobScrapRepository;
import com.shplatform.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class JobScrapServiceImplTest {

    @Mock
    private JobScrapRepository jobScrapRepository;

    @Mock
    private JobPostingRepository jobPostingRepository;

    @InjectMocks
    private JobScrapServiceImpl jobScrapService;

    private final Long userId = 6L;

    @Test
    @DisplayName("공고를 스크랩하면 저장된다")
    void scrap_savesNewScrap() {
        given(jobScrapRepository.existsByUserIdAndPostingId(userId, 1L)).willReturn(false);
        JobPosting posting = postingOf(1L, "네이버", "백엔드");
        given(jobPostingRepository.findById(1L)).willReturn(Optional.of(posting));

        jobScrapService.scrap(userId, 1L);

        ArgumentCaptor<JobScrap> captor = ArgumentCaptor.forClass(JobScrap.class);
        then(jobScrapRepository).should().save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getPostingId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("이미 스크랩된 공고면 중복 저장하지 않는다")
    void scrap_skipsDuplicate() {
        given(jobScrapRepository.existsByUserIdAndPostingId(userId, 1L)).willReturn(true);

        jobScrapService.scrap(userId, 1L);

        then(jobScrapRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 공고 스크랩 시 예외가 발생한다")
    void scrap_throwsWhenPostingMissing() {
        given(jobScrapRepository.existsByUserIdAndPostingId(userId, 999L)).willReturn(false);
        given(jobPostingRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> jobScrapService.scrap(userId, 999L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("스크랩 해제 시 해당 행을 삭제한다")
    void unscrap_deletesExisting() {
        JobScrap scrap = JobScrap.builder().id(10L).userId(userId).postingId(1L).build();
        given(jobScrapRepository.findByUserIdAndPostingId(userId, 1L))
                .willReturn(Optional.of(scrap));

        jobScrapService.unscrap(userId, 1L);

        then(jobScrapRepository).should().delete(scrap);
    }

    @Test
    @DisplayName("스크랩이 없으면 해제 요청이 무시된다")
    void unscrap_ignoresWhenAbsent() {
        given(jobScrapRepository.findByUserIdAndPostingId(userId, 1L))
                .willReturn(Optional.empty());

        jobScrapService.unscrap(userId, 1L);

        then(jobScrapRepository).should(never()).delete(any());
    }

    @Test
    @DisplayName("내 스크랩 목록에 공고 정보가 함께 담긴다")
    void getMyScraps_returnsResponsesWithPostingInfo() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 24, 12, 0);
        JobScrap scrap = JobScrap.builder().id(10L).userId(userId).postingId(1L)
                .createdAt(now).build();
        given(jobScrapRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .willReturn(List.of(scrap));
        given(jobPostingRepository.findAllById(List.of(1L)))
                .willReturn(List.of(postingOf(1L, "네이버", "백엔드 개발자")));

        List<JobScrapResponse> result = jobScrapService.getMyScraps(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).company()).isEqualTo("네이버");
        assertThat(result.get(0).position()).isEqualTo("백엔드 개발자");
        assertThat(result.get(0).scrappedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("원본 공고가 삭제된 스크랩도 목록에 포함된다")
    void getMyScraps_includesOrphanScrap() {
        JobScrap scrap = JobScrap.builder().id(11L).userId(userId).postingId(2L)
                .createdAt(LocalDateTime.now()).build();
        given(jobScrapRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .willReturn(List.of(scrap));
        given(jobPostingRepository.findAllById(List.of(2L))).willReturn(List.of());

        List<JobScrapResponse> result = jobScrapService.getMyScraps(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).company()).isEqualTo("(삭제된 공고)");
    }

    @Test
    @DisplayName("스크랩 여부를 확인한다")
    void isScrapped_returnsExistence() {
        given(jobScrapRepository.existsByUserIdAndPostingId(userId, 1L)).willReturn(true);
        given(jobScrapRepository.existsByUserIdAndPostingId(userId, 2L)).willReturn(false);

        assertThat(jobScrapService.isScrapped(userId, 1L)).isTrue();
        assertThat(jobScrapService.isScrapped(userId, 2L)).isFalse();
    }

    private JobPosting postingOf(Long id, String company, String position) {
        return JobPosting.builder()
                .id(id)
                .siteName("saramin")
                .url("https://example.com/" + id)
                .company(company)
                .position(position)
                .build();
    }
}
