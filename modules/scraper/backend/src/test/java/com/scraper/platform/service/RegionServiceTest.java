package com.scraper.platform.service;

import com.scraper.platform.model.Region;
import com.scraper.platform.repository.RegionRepository;
import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegionService 테스트")
class RegionServiceTest {

    @Mock
    private RegionRepository regionRepository;

    @InjectMocks
    private RegionService regionService;

    private Region region(Long id, String name, Integer displayOrder, Boolean isActive) {
        return Region.builder()
                .id(id)
                .name(name)
                .displayOrder(displayOrder)
                .isActive(isActive)
                .build();
    }

    @Nested
    @DisplayName("create 메서드")
    class Create {

        @Test
        @DisplayName("지역을 생성한다")
        void 생성() {
            // given
            Region request = region(null, "울릉도", null, null);
            given(regionRepository.existsByName("울릉도")).willReturn(false);
            given(regionRepository.save(request)).willAnswer(inv -> {
                request.setId(99L);
                return request;
            });

            // when
            Region result = regionService.create(request);

            // then
            assertEquals(99L, result.getId());
            assertEquals(0, result.getDisplayOrder());
            assertTrue(result.getIsActive());
        }

        @Test
        @DisplayName("중복 이름이면 DUPLICATE_NAME 예외를 던진다")
        void 중복이름_예외() {
            // given
            Region request = region(null, "서울", 1, true);
            given(regionRepository.existsByName("서울")).willReturn(true);

            // when / then
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> regionService.create(request));
            assertEquals(ErrorCode.DUPLICATE_NAME, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("update 메서드")
    class Update {

        @Test
        @DisplayName("지역 정보를 수정한다")
        void 수정() {
            // given
            Region existing = region(1L, "서울", 1, true);
            Region request = region(null, "서울특별시", 2, false);
            given(regionRepository.findById(1L)).willReturn(Optional.of(existing));
            given(regionRepository.existsByName("서울특별시")).willReturn(false);
            given(regionRepository.save(existing)).willReturn(existing);

            // when
            Region result = regionService.update(1L, request);

            // then
            assertEquals("서울특별시", result.getName());
            assertEquals(2, result.getDisplayOrder());
            assertFalse(result.getIsActive());
        }

        @Test
        @DisplayName("없는 ID면 NOT_FOUND 예외를 던진다")
        void 없는ID_예외() {
            // given
            given(regionRepository.findById(999L)).willReturn(Optional.empty());

            // when / then
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> regionService.update(999L, region(null, "서울", 1, true)));
            assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("이름 변경 시 중복이면 DUPLICATE_NAME 예외를 던진다")
        void 이름변경중복_예외() {
            // given
            Region existing = region(1L, "서울", 1, true);
            Region request = region(null, "경기", 2, true);
            given(regionRepository.findById(1L)).willReturn(Optional.of(existing));
            given(regionRepository.existsByName("경기")).willReturn(true);

            // when / then
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> regionService.update(1L, request));
            assertEquals(ErrorCode.DUPLICATE_NAME, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("delete 메서드")
    class Delete {

        @Test
        @DisplayName("지역을 삭제한다")
        void 삭제() {
            // given
            given(regionRepository.existsById(1L)).willReturn(true);

            // when
            regionService.delete(1L);

            // then
            verify(regionRepository).deleteById(1L);
        }

        @Test
        @DisplayName("없는 ID면 NOT_FOUND 예외를 던진다")
        void 없는ID_예외() {
            // given
            given(regionRepository.existsById(999L)).willReturn(false);

            // when / then
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> regionService.delete(999L));
            assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("조회 메서드")
    class Query {

        @Test
        @DisplayName("활성 지역 목록을 표시 순서로 조회한다")
        void 활성목록_조회() {
            // given
            List<Region> regions = Arrays.asList(
                    region(1L, "서울", 1, true),
                    region(2L, "경기", 2, true));
            given(regionRepository.findByIsActiveTrueOrderByDisplayOrderAsc()).willReturn(regions);

            // when
            List<Region> result = regionService.getActiveRegions();

            // then
            assertEquals(2, result.size());
            assertEquals("서울", result.get(0).getName());
        }

        @Test
        @DisplayName("이름 부분 일치로 검색한다")
        void 검색() {
            // given
            List<Region> regions = List.of(region(1L, "서울", 1, true));
            given(regionRepository.findTop20ByNameContainingOrderByNameAsc("서")).willReturn(regions);

            // when
            List<Region> result = regionService.search("서");

            // then
            assertEquals(1, result.size());
            assertEquals("서울", result.get(0).getName());
        }
    }
}