package com.scraper.platform.service;

import com.scraper.platform.model.BlockReason;
import com.scraper.platform.model.CompanyBlacklist;
import com.scraper.platform.model.CompanyNote;
import com.scraper.platform.repository.CompanyBlacklistRepository;
import com.scraper.platform.repository.CompanyNoteRepository;
import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompanyBlacklistService 테스트")
class CompanyBlacklistServiceTest {

    @Mock
    private CompanyBlacklistRepository repository;

    @Mock
    private BlockReasonService blockReasonService;

    @Mock
    private CompanyNoteRepository noteRepository;

    @InjectMocks
    private CompanyBlacklistService service;

    @Nested
    @DisplayName("add 메서드")
    class Add {

        @Test
        @DisplayName("새 회사를 카테고리와 함께 등록한다")
        void add_shouldCreateWithCategories() {
            // given
            var start = BlockReason.of("스타트업 X", "company_type", 1, true);
            var reason = BlockReason.of("연봉·복지 협상 불가", "reason", 10, true);
            given(repository.findByAccountIdOrderByCreatedAtDesc(1L)).willReturn(List.of());
            given(blockReasonService.resolveCategories(List.of(1L, 2L), null))
                    .willReturn(List.of(start, reason));
            given(repository.save(any(CompanyBlacklist.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            var result = service.add(1L, "  (주)테스트 회사 ", "메모", List.of(1L, 2L), null, null);

            // then
            assertEquals("테스트회사", result.getCompanyNameNormalized());
            assertEquals("메모", result.getReason());
            assertEquals(2, result.getBlockReasons().size());
            assertEquals("스타트업 X", result.getBlockReasons().get(0).getName());
            assertTrue(result.getBlockReasons().stream().anyMatch(r -> r.getName().equals("연봉·복지 협상 불가")));
        }

        @Test
        @DisplayName("중복 등록이면 카테고리를 갱신한다")
        void add_shouldUpdateCategoriesOnDuplicate() {
            // given
            var existing = CompanyBlacklist.builder()
                    .id(9L).accountId(1L)
                    .companyNameNormalized("테스트회사")
                    .reason("기존 사유")
                    .build();
            var category = BlockReason.of("대기업", "company_type", 3, true);
            given(repository.findByAccountIdOrderByCreatedAtDesc(1L)).willReturn(List.of(existing));
            given(blockReasonService.resolveCategories(List.of(3L), null)).willReturn(List.of(category));
            given(repository.save(any(CompanyBlacklist.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            var result = service.add(1L, "테스트 회사", null, List.of(3L), null, null);

            // then
            assertEquals(9L, result.getId());
            assertNull(result.getReason());
            assertEquals(1, result.getBlockReasons().size());
            verify(repository).save(any(CompanyBlacklist.class));
        }

        @Test
        @DisplayName("카테고리 없이 등록하면 빈 목록을 유지한다")
        void add_shouldKeepEmptyCategories() {
            // given
            given(repository.findByAccountIdOrderByCreatedAtDesc(1L)).willReturn(List.of());
            given(blockReasonService.resolveCategories(null, null)).willReturn(List.of());
            given(repository.save(any(CompanyBlacklist.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            var result = service.add(1L, "회사A", null, null, null, null);

            // then
            assertEquals("회사a", result.getCompanyNameNormalized());
            assertTrue(result.getBlockReasons().isEmpty());
        }

        @Test
        @DisplayName("사용자 신규 카테고리는 마스터로 승격되어 함께 저장된다")
        void add_shouldPromoteUserCategory() {
            // given
            var promoted = BlockReason.of("스타트업", "user", 20, true);
            given(repository.findByAccountIdOrderByCreatedAtDesc(1L)).willReturn(List.of());
            given(blockReasonService.resolveCategories(List.of(), List.of("스타트업")))
                    .willReturn(List.of(promoted));
            given(repository.save(any(CompanyBlacklist.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            var result = service.add(1L, "회사A", null, List.of(), List.of("스타트업"), null);

            // then
            assertEquals(1, result.getBlockReasons().size());
            assertEquals("스타트업", result.getBlockReasons().get(0).getName());
        }

        @Test
        @DisplayName("매칭 방식을 저장한다 (null이면 exact 기본값)")
        void add_shouldStoreMatchType() {
            // given
            given(repository.findByAccountIdOrderByCreatedAtDesc(1L)).willReturn(List.of());
            given(blockReasonService.resolveCategories(null, null)).willReturn(List.of());
            given(repository.save(any(CompanyBlacklist.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            var contains = service.add(1L, "회사B", null, null, null, CompanyBlacklist.MatchType.contains);
            var defaulted = service.add(1L, "회사C", null, null, null, null);

            // then
            assertEquals(CompanyBlacklist.MatchType.contains, contains.getMatchType());
            assertEquals(CompanyBlacklist.MatchType.exact, defaulted.getMatchType());
        }

        @Test
        @DisplayName("중복 등록 시 매칭 방식이 주어지면 갱신하고 null이면 기존을 유지한다")
        void add_shouldUpdateMatchTypeOnDuplicate() {
            // given
            var existing = CompanyBlacklist.builder()
                    .id(9L).accountId(1L)
                    .companyNameNormalized("테스트회사")
                    .matchType(CompanyBlacklist.MatchType.exact)
                    .build();
            given(repository.findByAccountIdOrderByCreatedAtDesc(1L)).willReturn(List.of(existing));
            given(blockReasonService.resolveCategories(null, null)).willReturn(List.of());
            given(repository.save(any(CompanyBlacklist.class))).willAnswer(inv -> inv.getArgument(0));

            // when - null이면 유지
            var keep = service.add(1L, "테스트 회사", null, null, null, null);
            assertEquals(CompanyBlacklist.MatchType.exact, keep.getMatchType());
            // when - 값이 있으면 갱신 (같은 기존 인스턴스가 갱신되므로 즉시 검증)
            var change = service.add(1L, "테스트 회사", null, null, null, CompanyBlacklist.MatchType.contains);

            // then
            assertEquals(CompanyBlacklist.MatchType.contains, change.getMatchType());
        }
    }

    @Nested
    @DisplayName("update 메서드")
    class Update {

        @Test
        @DisplayName("본인 항목의 카테고리를 교체하고 메모를 보존한다")
        void update_shouldReplaceCategories() {
            // given
            var existing = CompanyBlacklist.builder()
                    .id(9L).accountId(1L)
                    .companyNameNormalized("테스트회사")
                    .reason("기존 사유")
                    .build();
            var category = BlockReason.of("외국계", "company_type", 4, true);
            given(repository.findById(9L)).willReturn(java.util.Optional.of(existing));
            given(blockReasonService.resolveCategories(List.of(4L), null)).willReturn(List.of(category));
            given(repository.save(any(CompanyBlacklist.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            var result = service.update(1L, 9L, List.of(4L), null);

            // then
            assertEquals(9L, result.getId());
            assertEquals("기존 사유", result.getReason());
            assertEquals(1, result.getBlockReasons().size());
            assertEquals("외국계", result.getBlockReasons().get(0).getName());
            verify(repository).save(any(CompanyBlacklist.class));
        }

        @Test
        @DisplayName("타인 항목이면 null을 반환한다")
        void update_shouldIgnoreOtherOwner() {
            // given
            var other = CompanyBlacklist.builder()
                    .id(9L).accountId(2L)
                    .companyNameNormalized("다른유저")
                    .build();
            given(repository.findById(9L)).willReturn(java.util.Optional.of(other));

            // when
            var result = service.update(1L, 9L, List.of(4L), null);

            // then
            assertNull(result);
        }

        @Test
        @DisplayName("키워드 변경 시 연결된 회사 메모도 함께 이관된다")
        void update_shouldRenameKeywordAndMigrateNote() {
            // given
            var existing = CompanyBlacklist.builder()
                    .id(9L).accountId(1L)
                    .companyNameNormalized("구회사")
                    .build();
            var note = CompanyNote.builder()
                    .id(3L).accountId(1L)
                    .companyNameNormalized("구회사").companyNameDisplay("구회사")
                    .build();
            given(repository.findById(9L)).willReturn(java.util.Optional.of(existing));
            given(repository.findByAccountIdOrderByCreatedAtDesc(1L)).willReturn(List.of(existing));
            given(blockReasonService.resolveCategories(List.of(), null)).willReturn(List.of());
            given(noteRepository.findByAccountIdAndCompanyNameNormalized(1L, "신회사"))
                    .willReturn(java.util.Optional.empty());
            given(noteRepository.findByAccountIdAndCompanyNameNormalized(1L, "구회사"))
                    .willReturn(java.util.Optional.of(note));
            given(repository.save(any(CompanyBlacklist.class))).willAnswer(inv -> inv.getArgument(0));
            given(noteRepository.save(any(CompanyNote.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            var result = service.update(1L, 9L, "신회사", null, null, List.of(), null);

            // then
            assertEquals("신회사", result.getCompanyNameNormalized());
            assertEquals("신회사", note.getCompanyNameNormalized());
            assertEquals("신회사", note.getCompanyNameDisplay());
            verify(noteRepository).save(note);
        }

        @Test
        @DisplayName("변경 후 키워드가 다른 내 차단과 충돌하면 DUPLICATE_NAME 예외를 던진다")
        void update_shouldRejectDuplicateKeyword() {
            // given
            var existing = CompanyBlacklist.builder()
                    .id(9L).accountId(1L)
                    .companyNameNormalized("구회사")
                    .build();
            var other = CompanyBlacklist.builder()
                    .id(10L).accountId(1L)
                    .companyNameNormalized("신회사")
                    .build();
            given(repository.findById(9L)).willReturn(java.util.Optional.of(existing));
            given(repository.findByAccountIdOrderByCreatedAtDesc(1L)).willReturn(List.of(existing, other));
            given(blockReasonService.resolveCategories(null, null)).willReturn(List.of());

            // when / then
            var ex = assertThrows(BusinessException.class, () ->
                    service.update(1L, 9L, "신회사", null, null, null, null));
            assertEquals(ErrorCode.DUPLICATE_NAME, ex.getErrorCode());
        }

        @Test
        @DisplayName("매칭 방식을 변경한다 (null이면 기존 유지)")
        void update_shouldChangeMatchType() {
            // given
            var existing = CompanyBlacklist.builder()
                    .id(9L).accountId(1L)
                    .companyNameNormalized("구회사")
                    .matchType(CompanyBlacklist.MatchType.exact)
                    .build();
            given(repository.findById(9L)).willReturn(java.util.Optional.of(existing));
            given(blockReasonService.resolveCategories(null, null)).willReturn(List.of());
            given(repository.save(any(CompanyBlacklist.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            var keep = service.update(1L, 9L, null, null, null, null, null);
            assertEquals(CompanyBlacklist.MatchType.exact, keep.getMatchType());
            var change = service.update(1L, 9L, null, null, CompanyBlacklist.MatchType.contains, null, null);

            // then
            assertEquals(CompanyBlacklist.MatchType.contains, change.getMatchType());
        }
    }

    @Nested
    @DisplayName("stats 메서드")
    class Stats {

        @Test
        @DisplayName("카테고리별 사용 빈도와 카테고리 없는 수를 집계한다")
        void stats_shouldAggregateByCategory() {
            // given
            var startUp = BlockReason.of("스타트업 X", "company_type", 1, true);
            var regular = BlockReason.of("연봉·복지 협상 불가", "reason", 10, true);
            var companyBh = CompanyBlacklist.builder()
                    .id(1L).accountId(1L).companyNameNormalized("회사A")
                    .blockReasons(new java.util.ArrayList<>(List.of(startUp, regular)))
                    .build();
            var companyB = CompanyBlacklist.builder()
                    .id(2L).accountId(1L).companyNameNormalized("회사B")
                    .blockReasons(new java.util.ArrayList<>(List.of(startUp)))
                    .build();
            var companyC = CompanyBlacklist.builder()
                    .id(3L).accountId(1L).companyNameNormalized("회사C")
                    .build();
            given(repository.findByAccountIdOrderByCreatedAtDesc(1L))
                    .willReturn(List.of(companyBh, companyB, companyC));

            // when
            var result = service.stats(1L);

            // then
            assertEquals(3, result.total());
            assertEquals(1, result.uncategorized());
            assertEquals(2, result.categories().size());
            var first = result.categories().get(0);
            assertEquals("스타트업 X", first.name());
            assertEquals("company_type", first.category());
            assertEquals(2, first.count());
            var second = result.categories().get(1);
            assertEquals("연봉·복지 협상 불가", second.name());
            assertEquals(1, second.count());
        }

        @Test
        @DisplayName("블랙리스트가 없으면 0과 빈 목록을 반환한다")
        void stats_shouldHandleEmptyList() {
            // given
            given(repository.findByAccountIdOrderByCreatedAtDesc(1L)).willReturn(List.of());

            // when
            var result = service.stats(1L);

            // then
            assertEquals(0, result.total());
            assertEquals(0, result.uncategorized());
            assertTrue(result.categories().isEmpty());
        }
    }

    @Nested
    @DisplayName("matcher/BlockMatcher (설계 036 — 판정 단일 소스)")
    class BlockMatcherTest {

        private CompanyBlacklist entry(String normalized, CompanyBlacklist.MatchType type) {
            return CompanyBlacklist.builder().id(1L).accountId(1L)
                    .companyNameNormalized(normalized).matchType(type).build();
        }

        @Test
        @DisplayName("exact 항목은 정규화명 정확 일치만 차단한다")
        void exact_정확일치만_차단() {
            var matcher = CompanyBlacklistService.BlockMatcher.of(
                    List.of(entry("삼성전자", CompanyBlacklist.MatchType.exact)));

            assertTrue(matcher.isBlocked("삼성전자"));
            assertFalse(matcher.isBlocked("삼성전자서비스"));
            assertFalse(matcher.isBlocked("삼성"));
            assertFalse(matcher.isBlocked(""));
            assertFalse(matcher.isBlocked(null));
        }

        @Test
        @DisplayName("contains 항목은 부분 일치도 차단한다")
        void contains_부분일치_차단() {
            var matcher = CompanyBlacklistService.BlockMatcher.of(
                    List.of(entry("악덕", CompanyBlacklist.MatchType.contains)));

            assertTrue(matcher.isBlocked("악덕기업"));
            assertTrue(matcher.isBlocked("주식회사악덕"));
            assertTrue(matcher.isBlocked("악덕"));
            assertFalse(matcher.isBlocked("착한기업"));
            assertFalse(matcher.isBlocked(""));
            assertFalse(matcher.isBlocked(null));
        }

        @Test
        @DisplayName("matchType null(레거시)은 exact로 간주한다")
        void null_matchType은_exact() {
            var matcher = CompanyBlacklistService.BlockMatcher.of(
                    List.of(entry("레거시", null)));

            assertTrue(matcher.isBlocked("레거시"));
            assertFalse(matcher.isBlocked("레거시회사"));
        }

        @Test
        @DisplayName("혼용 목록에서 일치하는 항목을 반환하고(최신 우선), 매칭 방식을 그대로 노출한다")
        void 혼용_첫일치_반환() {
            var containsEntry = entry("테스트", CompanyBlacklist.MatchType.contains);
            var exactEntry = entry("테스트", CompanyBlacklist.MatchType.exact);
            // 등록 역순(최신 우선): exact가 먼저 온다
            var matcher = CompanyBlacklistService.BlockMatcher.of(List.of(exactEntry, containsEntry));

            // 정확 일치 → exact 항목이 최신 우선으로 매칭
            assertSame(exactEntry, matcher.firstMatch("테스트"));
            // exact 불일치 + contains 일치 → contains 항목 매칭
            var hitContains = matcher.firstMatch("테스트회사");
            assertSame(containsEntry, hitContains);
            assertEquals(CompanyBlacklist.MatchType.contains, hitContains.getMatchType());
            assertNull(matcher.firstMatch("다른회사"));
        }

        @Test
        @DisplayName("항목이 없으면 isEmpty이고 어떤 이름도 차단하지 않는다")
        void 빈목록() {
            var matcher = CompanyBlacklistService.BlockMatcher.of(List.of());

            assertTrue(matcher.isEmpty());
            assertFalse(matcher.isBlocked("아무회사"));
            assertNull(matcher.firstMatch("아무회사"));
        }

        @Test
        @DisplayName("of(null)은 빈 목록으로 정규화한다")
        void of_null_정규화() {
            assertTrue(CompanyBlacklistService.BlockMatcher.of(null).isEmpty());
        }
    }
}
