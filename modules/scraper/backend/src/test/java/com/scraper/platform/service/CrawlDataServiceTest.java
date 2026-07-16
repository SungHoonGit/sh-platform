package com.scraper.platform.service;

import com.scraper.platform.model.CrawlData;
import com.scraper.platform.repository.CrawlDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CrawlDataService 테스트")
class CrawlDataServiceTest {

    @Mock
    private CrawlDataRepository crawlDataRepository;

    @InjectMocks
    private CrawlDataService crawlDataService;

    private CrawlData testCrawlData;

    @BeforeEach
    void setUp() {
        testCrawlData = CrawlData.builder()
                .id(1L)
                .category("java")
                .filePath("/data/java/2026-07-14/test.md")
                .fileName("test.md")
                .title("테스트 문서")
                .build();
    }

    @Nested
    @DisplayName("getCrawlDataByCategory 메서드")
    class GetCrawlDataByCategory {

        @Test
        @DisplayName("카테고리별 데이터를 조회한다")
        void getCrawlDataByCategory_shouldReturnData() {
            // given
            PageRequest pageRequest = PageRequest.of(0, 20);
            Page<CrawlData> page = new PageImpl<>(List.of(testCrawlData));
            given(crawlDataRepository.findByCategory("java", pageRequest)).willReturn(page);

            // when
            var result = crawlDataService.getCrawlDataByCategory("java", pageRequest);

            // then
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
        }
    }

    @Nested
    @DisplayName("searchCrawlData 메서드")
    class SearchCrawlData {

        @Test
        @DisplayName("키워드로 검색한다")
        void searchCrawlData_shouldReturnMatchingData() {
            // given
            PageRequest pageRequest = PageRequest.of(0, 20);
            Page<CrawlData> page = new PageImpl<>(List.of(testCrawlData));
            given(crawlDataRepository.searchByKeyword("테스트", pageRequest)).willReturn(page);

            // when
            var result = crawlDataService.searchCrawlData("테스트", pageRequest);

            // then
            assertNotNull(result);
            assertEquals(1, result.getContent().size());
        }
    }

    @Nested
    @DisplayName("getCountByCategory 메서드")
    class GetCountByCategory {

        @Test
        @DisplayName("카테고리별 데이터 수를 조회한다")
        void getCountByCategory_shouldReturnCount() {
            // given
            given(crawlDataRepository.countByCategory("java")).willReturn(10L);

            // when
            var result = crawlDataService.getCountByCategory("java");

            // then
            assertEquals(10L, result);
        }
    }
}
