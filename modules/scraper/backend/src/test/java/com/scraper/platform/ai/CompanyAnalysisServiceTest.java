package com.scraper.platform.ai;

import com.scraper.platform.model.CompanyBlacklist;
import com.scraper.platform.model.CompanyNote;
import com.scraper.platform.model.JobPosting;
import com.scraper.platform.repository.CompanyBlacklistRepository;
import com.scraper.platform.repository.CompanyNoteRepository;
import com.scraper.platform.repository.CompanyRatingRepository;
import com.scraper.platform.repository.JobPostingRepository;
import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CompanyAnalysisServiceTest {

    @Mock CompanyNoteRepository noteRepository;
    @Mock JobPostingRepository jobPostingRepository;
    @Mock CompanyRatingRepository ratingRepository;
    @Mock CompanyBlacklistRepository blacklistRepository;

    @Test
    void throws_when_llm_not_configured() {
        CompanyAnalysisService service = service(new LlmProperties("", "", "", null, 30), List.of());
        BusinessException ex = assertThrows(BusinessException.class, () -> service.analyze(1L, 10L));
        assertEquals(ErrorCode.INTERNAL_ERROR, ex.getErrorCode());
    }

    @Test
    void generates_markdown_with_db_citations() {
        CompanyNote note = CompanyNote.builder().id(10L).accountId(1L).companyNameDisplay("삼성전자")
                .companyNameNormalized("삼성전자").myStars(4).isBookmarked(true).noteMd("기존 메모").build();
        given(noteRepository.findById(10L)).willReturn(Optional.of(note));
        given(jobPostingRepository.findRecentByNormalizedCompany(eq("삼성전자"), any(PageRequest.class))).willReturn(List.of(
                JobPosting.builder().position("백엔드 개발자").siteName("wanted").company("삼성전자")
                        .url("https://example.com/1").crawledAt(LocalDate.of(2026, 11, 1)).build()
        ));
        given(ratingRepository.findByCompanyNameIn(any())).willReturn(List.of());
        given(blacklistRepository.findByAccountIdAndCompanyNameNormalized(1L, "삼성전자")).willReturn(Optional.empty());

        LlmClient client = new LlmClient() {
            public boolean supports(String provider) { return "gemini".equalsIgnoreCase(provider); }
            public String generate(String prompt, LlmProperties properties) { return "## 요약\nDB 근거 기반 분석"; }
        };
        CompanyAnalysisService service = service(new LlmProperties("gemini", "test-model", "k", "https://x", 30), List.of(client));

        CompanyAnalysisResult result = service.analyze(1L, 10L);

        assertEquals("삼성전자", result.companyName());
        assertTrue(result.markdown().contains("근거"));
        assertEquals(1, result.citations().size());
        assertEquals("test-model", result.model());
    }

    @Test
    void throws_not_found_for_other_account() {
        CompanyNote note = CompanyNote.builder().id(10L).accountId(9L).companyNameDisplay("A")
                .companyNameNormalized("a").build();
        given(noteRepository.findById(10L)).willReturn(Optional.of(note));
        CompanyAnalysisService service = service(new LlmProperties("gemini", "m", "k", null, 30), List.of());
        assertEquals(ErrorCode.NOT_FOUND, assertThrows(BusinessException.class, () -> service.analyze(1L, 10L)).getErrorCode());
    }

    private CompanyAnalysisService service(LlmProperties properties, List<LlmClient> clients) {
        return new CompanyAnalysisService(noteRepository, jobPostingRepository, ratingRepository, blacklistRepository, properties, clients);
    }
}
