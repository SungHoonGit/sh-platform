package com.scraper.platform.service;

import com.scraper.platform.api.dto.CompanyNoteDetailResponse;
import com.scraper.platform.api.dto.CompanyNoteRequest;
import com.scraper.platform.api.dto.CompanyNoteResponse;
import com.scraper.platform.model.CompanyBlacklist;
import com.scraper.platform.model.CompanyNote;
import com.scraper.platform.model.CompanyRating;
import com.scraper.platform.repository.CompanyBlacklistRepository;
import com.scraper.platform.repository.CompanyNoteRepository;
import com.scraper.platform.repository.CompanyRatingRepository;
import com.scraper.platform.repository.JobPostingRepository;
import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompanyNoteService 테스트")
class CompanyNoteServiceTest {

    @Mock
    private CompanyNoteRepository noteRepository;

    @Mock
    private CompanyBlacklistRepository blacklistRepository;

    @Mock
    private CompanyRatingRepository ratingRepository;

    @Mock
    private JobPostingRepository jobPostingRepository;

    @Mock
    private BlockReasonService blockReasonService;

    @InjectMocks
    private CompanyNoteService noteService;

    private static final Long ACCOUNT = 7L;

    private CompanyNote note(Long id, String normalized, String display,
                             Integer stars, Boolean bookmarked, String md) {
        return CompanyNote.builder()
                .id(id)
                .accountId(ACCOUNT)
                .companyNameNormalized(normalized)
                .companyNameDisplay(display)
                .myStars(stars)
                .isBookmarked(bookmarked)
                .noteMd(md)
                .build();
    }

    private CompanyRating rating(String name, Double avg, Double planet, Double korea, Double saramin) {
        return CompanyRating.builder()
                .companyName(name)
                .averageScore(avg)
                .jobplanetScore(planet)
                .jobkoreaScore(korea)
                .saraminScore(saramin)
                .build();
    }

    private void stubEmptyJoins() {
        given(blacklistRepository.findByAccountIdOrderByCreatedAtDesc(ACCOUNT)).willReturn(List.of());
        given(ratingRepository.findByCompanyNameIn(any())).willReturn(List.of());
    }

    @Nested
    @DisplayName("upsert 메서드")
    class Upsert {

        @Test
        @DisplayName("신규 회사는 정규화해 생성한다")
        void 신규_생성() {
            given(noteRepository.findByAccountIdAndCompanyNameNormalized(ACCOUNT, "삼성전자"))
                    .willReturn(Optional.empty());
            given(noteRepository.save(any())).willAnswer(inv -> {
                CompanyNote n = inv.getArgument(0);
                n.setId(99L);
                return n;
            });
            stubEmptyJoins();

            CompanyNoteDetailResponse result = noteService.upsert(ACCOUNT,
                    new CompanyNoteRequest("(주)삼성전자 ", 4, true, "## 총평", null, null));

            assertEquals(99L, result.id());
            assertEquals("(주)삼성전자", result.companyNameDisplay());
            assertEquals(4, result.myStars());
            assertTrue(result.bookmarked());
        }

        @Test
        @DisplayName("같은 회사는 갱신한다(멱등)")
        void 멱등_갱신() {
            CompanyNote existing = note(null, "카카오", "카카오", 3, false, null);
            existing.setId(5L);
            given(noteRepository.findByAccountIdAndCompanyNameNormalized(ACCOUNT, "카카오"))
                    .willReturn(Optional.of(existing));
            given(noteRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            stubEmptyJoins();

            CompanyNoteDetailResponse result = noteService.upsert(ACCOUNT,
                    new CompanyNoteRequest("카카오", 5, true, null, null, null));

            assertEquals(5L, result.id());
            assertEquals(5, result.myStars());
            assertTrue(result.bookmarked());
        }

        @Test
        @DisplayName("회사명 누락이면 INVALID_INPUT 예외를 던진다")
        void 회사명누락_예외() {
            BusinessException ex = assertThrows(BusinessException.class, () ->
                    noteService.upsert(ACCOUNT, new CompanyNoteRequest("  ", 3, null, null, null, null)));
            assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
        }

        @Test
        @DisplayName("별점 범위(1~5) 밖이면 INVALID_INPUT 예외를 던진다")
        void 별점범위_예외() {
            assertEquals(ErrorCode.INVALID_INPUT, assertThrows(BusinessException.class, () ->
                    noteService.upsert(ACCOUNT, new CompanyNoteRequest("네이버", 0, null, null, null, null))).getErrorCode());
            assertEquals(ErrorCode.INVALID_INPUT, assertThrows(BusinessException.class, () ->
                    noteService.upsert(ACCOUNT, new CompanyNoteRequest("네이버", 6, null, null, null, null))).getErrorCode());
        }

        @Test
        @DisplayName("차단된 회사는 북마크가 강제 해제된다")
        void 차단회사_북마크해제() {
            given(noteRepository.findByAccountIdAndCompanyNameNormalized(ACCOUNT, "악덕기업"))
                    .willReturn(Optional.empty());
            given(noteRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(blacklistRepository.existsByAccountIdAndCompanyNameNormalized(ACCOUNT, "악덕기업"))
                    .willReturn(true);
            stubEmptyJoins();

            CompanyNoteDetailResponse result = noteService.upsert(ACCOUNT,
                    new CompanyNoteRequest("악덕기업", 1, true, null, null, null));

            assertFalse(result.bookmarked());
        }

        @Test
        @DisplayName("별점 설정 시 북마크가 자동 ON된다")
        void 별점_북마크자동() {
            given(noteRepository.findByAccountIdAndCompanyNameNormalized(ACCOUNT, "카카오"))
                    .willReturn(Optional.empty());
            given(noteRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            stubEmptyJoins();

            CompanyNoteDetailResponse result = noteService.upsert(ACCOUNT,
                    new CompanyNoteRequest("카카오", 4, null, null, null, null));

            assertEquals(4, result.myStars());
            assertTrue(result.bookmarked());
        }

        @Test
        @DisplayName("차단된 회사에 별점을 달면 별점·북마크가 모두 해제된다")
        void 차단회사_별점해제() {
            given(noteRepository.findByAccountIdAndCompanyNameNormalized(ACCOUNT, "악덕기업"))
                    .willReturn(Optional.empty());
            given(noteRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(blacklistRepository.existsByAccountIdAndCompanyNameNormalized(ACCOUNT, "악덕기업"))
                    .willReturn(true);
            stubEmptyJoins();

            CompanyNoteDetailResponse result = noteService.upsert(ACCOUNT,
                    new CompanyNoteRequest("악덕기업", 5, null, null, null, null));

            assertNull(result.myStars());
            assertFalse(result.bookmarked());
        }

        @Test
        @DisplayName("태그 지정 시 공유 resolve로 저장된다")
        void 태그_저장() {
            var tag = com.scraper.platform.model.BlockReason.of("관심기업", "user", 20, true);
            given(noteRepository.findByAccountIdAndCompanyNameNormalized(ACCOUNT, "카카오"))
                    .willReturn(Optional.empty());
            given(noteRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(blockReasonService.resolveCategories(List.of(9L), List.of("관심기업")))
                    .willReturn(List.of(tag));
            stubEmptyJoins();

            CompanyNoteDetailResponse result = noteService.upsert(ACCOUNT,
                    new CompanyNoteRequest("카카오", null, null, null, List.of(9L), List.of("관심기업")));

            assertEquals(1, result.categories().size());
            assertEquals("관심기업", result.categories().get(0).name());
            verify(noteRepository).save(org.mockito.ArgumentMatchers.argThat(
                    n -> n.getNoteReasons().size() == 1));
        }
    }

    @Nested
    @DisplayName("update/delete 메서드")
    class UpdateDelete {

        @Test
        @DisplayName("별점·북마크·MD를 수정한다 (회사명 변경 무시)")
        void 수정() {
            CompanyNote existing = note(5L, "카카오", "카카오", 3, false, "구메모");
            given(noteRepository.findById(5L)).willReturn(Optional.of(existing));
            given(noteRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            stubEmptyJoins();

            CompanyNoteDetailResponse result = noteService.update(ACCOUNT, 5L,
                    new CompanyNoteRequest("다른이름", 5, true, "새메모", null, null));

            assertEquals("카카오", result.companyNameDisplay());
            assertEquals(5, result.myStars());
            assertTrue(result.bookmarked());
        }

        @Test
        @DisplayName("타인 소유 수정은 NOT_FOUND 예외를 던진다")
        void 타인수정_예외() {
            CompanyNote others = note(5L, "카카오", "카카오", 3, false, null);
            others.setAccountId(999L);
            given(noteRepository.findById(5L)).willReturn(Optional.of(others));

            assertEquals(ErrorCode.NOT_FOUND, assertThrows(BusinessException.class, () ->
                    noteService.update(ACCOUNT, 5L,
                            new CompanyNoteRequest(null, 5, null, null, null, null))).getErrorCode());
        }

        @Test
        @DisplayName("타인 소유 삭제는 NOT_FOUND 예외를 던진다")
        void 타인삭제_예외() {
            given(noteRepository.findById(5L)).willReturn(Optional.empty());

            assertEquals(ErrorCode.NOT_FOUND, assertThrows(BusinessException.class, () ->
                    noteService.delete(ACCOUNT, 5L)).getErrorCode());
        }

        @Test
        @DisplayName("본인 메모 삭제 시 repository.delete를 호출한다")
        void 삭제_호출() {
            CompanyNote existing = note(5L, "카카오", "카카오", 3, false, null);
            given(noteRepository.findById(5L)).willReturn(Optional.of(existing));

            noteService.delete(ACCOUNT, 5L);

            verify(noteRepository).delete(existing);
        }

        @Test
        @DisplayName("차단된 회사 수정 시 북마크가 강제 해제된다")
        void 차단회사_수정_북마크해제() {
            CompanyNote existing = note(5L, "악덕기업", "악덕기업", 2, true, null);
            given(noteRepository.findById(5L)).willReturn(Optional.of(existing));
            given(noteRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(blacklistRepository.existsByAccountIdAndCompanyNameNormalized(ACCOUNT, "악덕기업"))
                    .willReturn(true);
            stubEmptyJoins();

            CompanyNoteDetailResponse result = noteService.update(ACCOUNT, 5L,
                    new CompanyNoteRequest(null, null, true, null, null, null));

            assertFalse(result.bookmarked());
        }

        @Test
        @DisplayName("북마크 해제 시 별도 함께 삭제된다")
        void 북마크해제_별삭제() {
            CompanyNote existing = note(5L, "카카오", "카카오", 4, true, null);
            given(noteRepository.findById(5L)).willReturn(Optional.of(existing));
            given(noteRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            stubEmptyJoins();

            CompanyNoteDetailResponse result = noteService.update(ACCOUNT, 5L,
                    new CompanyNoteRequest(null, null, false, null, null, null));

            assertFalse(result.bookmarked());
            assertNull(result.myStars());
        }
    }

    @Nested
    @DisplayName("list/get 조회")
    class ListGet {

        @Test
        @DisplayName("bookmarked 탭은 북마크 쿼리를 사용한다")
        void 북마크탭() {
            CompanyNote n = note(1L, "카카오", "카카오", 5, true, "메모");
            given(noteRepository.findByAccountIdAndIsBookmarkedTrue(eq(ACCOUNT), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(n)));
            stubEmptyJoins();

            Page<CompanyNoteResponse> page = noteService.list(ACCOUNT, "bookmarked", null, 0, 50, "updated", "desc");

            assertEquals(1, page.getTotalElements());
            assertTrue(page.getContent().get(0).bookmarked());
            assertTrue(page.getContent().get(0).hasNote());
        }

        @Test
        @DisplayName("blocked 탭은 블랙리스트 기준으로 메모·평점·숨김수를 조인한다")
        void 차단탭() {
            CompanyBlacklist blocked = CompanyBlacklist.builder()
                    .id(11L).accountId(ACCOUNT).companyNameNormalized("악덕기업").reason("야근")
                    .createdAt(java.time.LocalDateTime.of(2026, 9, 10, 12, 0)).build();
            given(blacklistRepository.findByAccountIdOrderByCreatedAtDesc(ACCOUNT))
                    .willReturn(List.of(blocked));
            given(noteRepository.findByAccountIdAndCompanyNameNormalizedIn(ACCOUNT, List.of("악덕기업")))
                    .willReturn(List.of());
            given(ratingRepository.findByCompanyNameIn(any())).willReturn(List.of());
            given(jobPostingRepository.countByNormalizedCompanyIn(List.of("악덕기업")))
                    .willReturn(java.util.Collections.singletonList(new Object[]{"악덕기업", 3L}));

            Page<CompanyNoteResponse> page = noteService.list(ACCOUNT, "blocked", null, 0, 50, "updated", "desc");

            assertEquals(1, page.getTotalElements());
            assertTrue(page.getContent().get(0).blocked());
            assertNull(page.getContent().get(0).id());
            assertEquals("악덕기업", page.getContent().get(0).companyNameNormalized());
            assertEquals(3L, page.getContent().get(0).hiddenCount());
            assertEquals(java.time.LocalDateTime.of(2026, 9, 10, 12, 0), page.getContent().get(0).updatedAt());
        }

        @Test
        @DisplayName("전체 탭은 메모 없는 차단 회사도 함께 표시한다")
        void 전체탭_차단포함() {
            CompanyNote n = note(1L, "카카오", "카카오", 5, true, "메모");
            given(noteRepository.findByAccountId(eq(ACCOUNT), any(Pageable.class)))
                    .willReturn(new PageImpl<>(List.of(n)));
            CompanyBlacklist blocked = CompanyBlacklist.builder()
                    .id(11L).accountId(ACCOUNT).companyNameNormalized("악덕기업").build();
            given(blacklistRepository.findByAccountIdOrderByCreatedAtDesc(ACCOUNT))
                    .willReturn(List.of(blocked));
            given(noteRepository.findByAccountIdAndCompanyNameNormalizedIn(ACCOUNT, List.of("악덕기업")))
                    .willReturn(List.of());
            given(ratingRepository.findByCompanyNameIn(any())).willReturn(List.of());
            given(jobPostingRepository.countByNormalizedCompanyIn(List.of("카카오")))
                    .willReturn(java.util.Collections.singletonList(new Object[]{"카카오", 12L}));
            given(jobPostingRepository.countByNormalizedCompanyIn(List.of("악덕기업")))
                    .willReturn(java.util.Collections.singletonList(new Object[]{"악덕기업", 7L}));

            Page<CompanyNoteResponse> page = noteService.list(ACCOUNT, "all", null, 0, 50, "updated", "desc");

            assertEquals(2, page.getTotalElements());
            assertEquals("카카오", page.getContent().get(0).companyNameDisplay());
            assertEquals(12L, page.getContent().get(0).hiddenCount());
            assertTrue(page.getContent().get(1).blocked());
            assertEquals(7L, page.getContent().get(1).hiddenCount());
        }

        @Test
        @DisplayName("상세 조회는 평점·차단여부를 포함한다")
        void 상세조회() {
            CompanyNote n = note(1L, "삼성전자", "삼성전자(주)", 4, true, "## 총평");
            given(noteRepository.findById(1L)).willReturn(Optional.of(n));
            given(blacklistRepository.findByAccountIdOrderByCreatedAtDesc(ACCOUNT)).willReturn(List.of());
            given(ratingRepository.findByCompanyNameIn(List.of("삼성전자(주)")))
                    .willReturn(List.of(rating("삼성전자(주)", 4.2, 4.1, 4.3, 4.2)));

            CompanyNoteDetailResponse detail = noteService.get(ACCOUNT, 1L);

            assertEquals(4.2, detail.averageScore());
            assertEquals(4.1, detail.jobplanetScore());
            assertFalse(detail.blocked());
            assertEquals("## 총평", detail.noteMd());
        }
    }

    @Nested
    @DisplayName("exportMarkdown 메서드")
    class Export {

        @Test
        @DisplayName("별점·북마크·평점·메모 섹션을 포함한 마크다운을 생성한다")
        void 내보내기() {
            CompanyNote n = note(1L, "삼성전자", "삼성전자(주)", 4, true, "## 총평\n- 복지 좋음");
            given(noteRepository.findById(1L)).willReturn(Optional.of(n));
            given(ratingRepository.findByCompanyNameIn(List.of("삼성전자(주)")))
                    .willReturn(List.of(rating("삼성전자(주)", 4.2, 4.1, 4.3, 4.2)));
            given(blacklistRepository.findByAccountIdAndCompanyNameNormalized(ACCOUNT, "삼성전자"))
                    .willReturn(Optional.empty());

            String md = noteService.exportMarkdown(ACCOUNT, 1L);

            assertTrue(md.startsWith("# 삼성전자(주)\n"));
            assertTrue(md.contains("★★★★☆ (4/5)"));
            assertTrue(md.contains("북마크: 예"));
            assertTrue(md.contains("차단: 아니오"));
            assertTrue(md.contains("4.2 (잡플래닛 4.1 · 잡코리아 4.3 · 사람인 4.2)"));
            assertTrue(md.contains("## 분석 메모"));
            assertTrue(md.contains("- 복지 좋음"));
        }
    }

    @Nested
    @DisplayName("recentPostings 메서드")
    class RecentPostings {

        @Test
        @DisplayName("정규화 회사명의 최근 공고를 size上限으로 반환한다")
        void 최근공고() {
            CompanyNote n = note(1L, "삼성전자", "삼성전자(주)", 4, true, null);
            given(noteRepository.findById(1L)).willReturn(Optional.of(n));
            var posting = com.scraper.platform.model.JobPosting.builder()
                    .id(100L).siteName("saramin").company("삼성전자(주)").position("백엔드")
                    .url("https://example.com/1")
                    .crawledAt(java.time.LocalDate.of(2026, 9, 16))
                    .build();
            given(jobPostingRepository.findRecentByNormalizedCompany(eq("삼성전자"), any(Pageable.class)))
                    .willReturn(List.of(posting));

            var result = noteService.recentPostings(ACCOUNT, 1L, 10);

            assertEquals(1, result.size());
            assertEquals("백엔드", result.get(0).position());
            assertEquals("saramin", result.get(0).siteName());
            verify(jobPostingRepository).findRecentByNormalizedCompany(eq("삼성전자"), any(Pageable.class));
        }

        @Test
        @DisplayName("타인 메모 조회는 NOT_FOUND 예외를 던진다")
        void 타인조회_예외() {
            given(noteRepository.findById(1L)).willReturn(Optional.empty());

            assertEquals(ErrorCode.NOT_FOUND, assertThrows(BusinessException.class, () ->
                    noteService.recentPostings(ACCOUNT, 1L, 10)).getErrorCode());
        }
    }

    @Nested
    @DisplayName("suggestCompanies 메서드")
    class Suggest {

        @Test
        @DisplayName("수집 회사명을 내 메모·차단 표시와 함께 반환한다")
        void 수집회사_제안() {
            CompanyNote n = note(1L, "삼성전자", "삼성전자(주)", 4, true, null);
            given(jobPostingRepository.findDistinctCompanyByCompanyContainingIgnoreCase(
                    eq("삼성"), any(Pageable.class)))
                    .willReturn(List.of("삼성전자(주)", "삼성SDS"));
            given(noteRepository.findByAccountIdAndCompanyNameNormalizedIn(
                    eq(ACCOUNT), any()))
                    .willReturn(List.of(n));
            given(blacklistRepository.findByAccountIdOrderByCreatedAtDesc(ACCOUNT))
                    .willReturn(List.of());

            var result = noteService.suggestCompanies(ACCOUNT, "삼성");

            assertEquals(2, result.size());
            assertEquals("삼성전자(주)", result.get(0).companyName());
            assertTrue(result.get(0).hasNote());
            assertEquals(1L, result.get(0).noteId());
            assertFalse(result.get(0).blocked());
            assertEquals("삼성SDS", result.get(1).companyName());
            assertFalse(result.get(1).hasNote());
            assertNull(result.get(1).noteId());
        }

        @Test
        @DisplayName("빈 검색어는 빈 목록을 반환한다 (DB 조회 없음)")
        void 빈검색어() {
            var result = noteService.suggestCompanies(ACCOUNT, "  ");

            assertTrue(result.isEmpty());
            verify(jobPostingRepository, org.mockito.Mockito.never())
                    .findDistinctCompanyByCompanyContainingIgnoreCase(
                            org.mockito.ArgumentMatchers.anyString(), any(Pageable.class));
        }
    }
}
