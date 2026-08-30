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
}
