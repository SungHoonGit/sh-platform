package com.scraper.platform.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.scraper.platform.api.dto.JobPostingExportItem;
import com.scraper.platform.api.dto.JobPostingExportResponse;
import com.scraper.platform.model.JobPosting;
import com.scraper.platform.repository.JobPostingRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * JobPostingExportController 단위 테스트 — 키워드/페이지네이션/필드 변환/size 제한을 검증한다.
 * <p>보안(API 키) 인증은 {@code ApiKeyFilter} 단위 테스트가 담당한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JobPostingExportController 테스트")
class JobPostingExportControllerTest {

    @Mock
    private JobPostingRepository jobPostingRepository;

    @InjectMocks
    private JobPostingExportController controller;

    private JobPosting testPosting;

    @BeforeEach
    void setUp() {
        testPosting = JobPosting.builder()
                .id(1L)
                .siteName("wanted")
                .company("(주)테스트")
                .position("Java 백엔드 개발자")
                .tech("Java, Spring Boot")
                .career("3년 이상")
                .location("서울")
                .deadline("2026-09-30")
                .url("https://example.com/1")
                .crawledAt(LocalDate.of(2026, 9, 10))
                .build();
    }

    @Nested
    @DisplayName("exportJobPostings 메서드")
    class ExportJobPostings {

        @Test
        @DisplayName("공고 목록을 export 응답으로 변환한다")
        void shouldMapPostingsToExportItems() {
            // given
            given(jobPostingRepository.searchExport(any(), any(PageRequest.class)))
                    .willReturn(new PageImpl<>(List.of(testPosting)));

            // when
            JobPostingExportResponse response = controller.exportJobPostings(null, 0, 100).getBody();

            // then
            assertNotNull(response);
            assertEquals(1, response.jobs().size());
            JobPostingExportItem item = response.jobs().get(0);
            assertEquals(1L, item.id());
            assertEquals("wanted", item.siteName());
            assertEquals("(주)테스트", item.company());
            assertEquals("Java 백엔드 개발자", item.position());
            assertEquals("Java, Spring Boot", item.tech());
            assertEquals("3년 이상", item.career());
            assertEquals("서울", item.location());
            assertEquals("2026-09-30", item.deadline());
            assertEquals("https://example.com/1", item.url());
            assertEquals(LocalDate.of(2026, 9, 10), item.crawledAt());
            assertEquals(1L, response.total());
        }

        @Test
        @DisplayName("keyword를 그대로 repository에 전달한다")
        void shouldPassKeywordToRepository() {
            // given
            given(jobPostingRepository.searchExport(anyString(), any(PageRequest.class)))
                    .willReturn(new PageImpl<>(List.of()));

            // when
            controller.exportJobPostings("자바", 1, 50).getBody();

            // then
            ArgumentCaptor<String> kwCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<PageRequest> prCaptor = ArgumentCaptor.forClass(PageRequest.class);
            verify(jobPostingRepository).searchExport(kwCaptor.capture(), prCaptor.capture());
            assertEquals("자바", kwCaptor.getValue());
            assertEquals(1, prCaptor.getValue().getPageNumber());
            assertEquals(50, prCaptor.getValue().getPageSize());
        }

        @Test
        @DisplayName("keyword가 공백이면 null로 정규화하여 전체를 조회한다")
        void shouldNormalizeBlankKeywordToNull() {
            // given
            given(jobPostingRepository.searchExport(any(), any(PageRequest.class)))
                    .willReturn(new PageImpl<>(List.of()));

            // when
            controller.exportJobPostings("   ", 0, 100).getBody();

            // then
            ArgumentCaptor<String> kwCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<PageRequest> prCaptor = ArgumentCaptor.forClass(PageRequest.class);
            verify(jobPostingRepository).searchExport(kwCaptor.capture(), prCaptor.capture());
            assertEquals(null, kwCaptor.getValue());
            assertEquals(0, prCaptor.getValue().getPageNumber());
            assertEquals(100, prCaptor.getValue().getPageSize());
        }

        @Test
        @DisplayName("size 상한(500)을 초과하면 500으로 제한한다")
        void shouldClampSizeTo500() {
            // given
            given(jobPostingRepository.searchExport(any(), any(PageRequest.class)))
                    .willReturn(new PageImpl<>(List.of()));

            // when
            JobPostingExportResponse response = controller.exportJobPostings(null, 0, 5000).getBody();

            // then
            assertEquals(500, response.size());
            ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
            verify(jobPostingRepository).searchExport(any(), captor.capture());
            assertEquals(500, captor.getValue().getPageSize());
        }

        @Test
        @DisplayName("size가 1 미만이면 1로 보정한다")
        void shouldClampSizeToMin1() {
            // given
            given(jobPostingRepository.searchExport(any(), any(PageRequest.class)))
                    .willReturn(new PageImpl<>(List.of()));

            // when
            JobPostingExportResponse response = controller.exportJobPostings(null, 0, 0).getBody();

            // then
            assertEquals(1, response.size());
            ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
            verify(jobPostingRepository).searchExport(any(), captor.capture());
            assertEquals(1, captor.getValue().getPageSize());
        }
    }
}