package com.scraper.platform.service;

import com.scraper.platform.model.BlockReason;
import com.scraper.platform.repository.BlockReasonRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("BlockReasonService 테스트")
class BlockReasonServiceTest {

    @Mock
    private BlockReasonRepository repository;

    @InjectMocks
    private BlockReasonService service;

    @Nested
    @DisplayName("resolveCategories 메서드")
    class ResolveCategories {

        @Test
        @DisplayName("기존 id와 신규 입력을 합쳐 정렬순으로 반환한다")
        void merge_and_sort() {
            var start = BlockReason.of("스타트업", "company_type", 1, true);
            var user = BlockReason.of("신규입력", "user", 20, true);
            given(repository.findAllById(List.of(1L))).willReturn(List.of(start));
            given(repository.findByName("신규입력")).willReturn(java.util.Optional.empty());
            given(repository.findTopByOrderBySortOrderDesc())
                    .willReturn(java.util.Optional.of(BlockReason.of("x", "user", 10, true)));
            given(repository.save(org.mockito.ArgumentMatchers.any(BlockReason.class))).willReturn(user);

            var result = service.resolveCategories(List.of(1L), List.of("신규입력"));

            assertEquals(2, result.size());
            assertEquals("스타트업", result.get(0).getName());
            assertEquals("신규입력", result.get(1).getName());
        }

        @Test
        @DisplayName("blank 입력은 무시하고 null 목록은 빈 목록으로 처리한다")
        void blank_ignored() {
            given(repository.findAllById(List.of())).willReturn(List.of());

            // List.of는 null 원소 불가라 Arrays.asList 사용 (서비스의 null 내성을 검증)
            var result = service.resolveCategories(List.of(), java.util.Arrays.asList("  ", null));

            assertEquals(0, result.size());
        }
    }
}
