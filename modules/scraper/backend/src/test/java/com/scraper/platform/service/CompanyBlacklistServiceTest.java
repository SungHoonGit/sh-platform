package com.scraper.platform.service;

import com.scraper.platform.model.BlockReason;
import com.scraper.platform.model.CompanyBlacklist;
import com.scraper.platform.repository.BlockReasonRepository;
import com.scraper.platform.repository.CompanyBlacklistRepository;
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
    private BlockReasonRepository blockReasonRepository;

    @Mock
    private BlockReasonService blockReasonService;

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
            given(blockReasonRepository.findAllById(List.of(1L, 2L))).willReturn(List.of(start, reason));
            given(repository.save(any(CompanyBlacklist.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            var result = service.add(1L, "  (주)테스트 회사 ", "메모", List.of(1L, 2L), null);

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
            given(blockReasonRepository.findAllById(List.of(3L))).willReturn(List.of(category));
            given(repository.save(any(CompanyBlacklist.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            var result = service.add(1L, "테스트 회사", null, List.of(3L), null);

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
            given(repository.save(any(CompanyBlacklist.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            var result = service.add(1L, "회사A", null, null, null);

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
            given(blockReasonService.ensureCategory("스타트업")).willReturn(promoted);
            given(blockReasonRepository.findAllById(List.<Long>of())).willReturn(List.of());
            given(repository.save(any(CompanyBlacklist.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            var result = service.add(1L, "회사A", null, List.of(), List.of("스타트업"));

            // then
            assertEquals(1, result.getBlockReasons().size());
            assertEquals("스타트업", result.getBlockReasons().get(0).getName());
            verify(blockReasonService).ensureCategory("스타트업");
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
            given(blockReasonRepository.findAllById(List.of(4L))).willReturn(List.of(category));
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
    @DisplayName("normalizedNames 메서드")
    class NormalizedNames {

        @Test
        @DisplayName("차단 회사의 정규화 이름 집합을 반환한다")
        void normalizedNames_shouldReturnCompanySet() {
            // given
            var a = CompanyBlacklist.builder()
                    .id(1L).accountId(1L).companyNameNormalized("회사a").build();
            var b = CompanyBlacklist.builder()
                    .id(2L).accountId(1L).companyNameNormalized("회사b").build();
            given(repository.findByAccountIdOrderByCreatedAtDesc(1L)).willReturn(List.of(a, b));

            // when
            var result = service.normalizedNames(1L);

            // then
            assertEquals(java.util.Set.of("회사a", "회사b"), result);
        }
    }
}
