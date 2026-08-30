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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("BlockReasonService 테스트")
class BlockReasonServiceTest {

    @Mock
    private BlockReasonRepository blockReasonRepository;

    @InjectMocks
    private BlockReasonService blockReasonService;

    @Nested
    @DisplayName("search 메서드")
    class Search {

        @Test
        @DisplayName("활성 사유를 이름으로 검색한다")
        void search_shouldReturnActiveReasons() {
            // given
            var reason = BlockReason.of("연봉·복지 협상 불가", "reason", 1, true);
            given(blockReasonRepository.findTop20ByNameContainingAndActiveTrueOrderBySortOrderAsc("연봉"))
                    .willReturn(List.of(reason));

            // when
            var result = blockReasonService.search("  연봉  ");

            // then
            assertEquals(1, result.size());
            assertEquals("연봉·복지 협상 불가", result.get(0).getName());
        }

        @Test
        @DisplayName("빈 키워드 또는 공백이면 빈 목록을 반환한다")
        void search_shouldReturnEmptyForBlankKeyword() {
            // when
            var blank = blockReasonService.search("   ");
            var empty = blockReasonService.search("");

            // then
            assertTrue(blank.isEmpty());
            assertTrue(empty.isEmpty());
        }
    }
}
