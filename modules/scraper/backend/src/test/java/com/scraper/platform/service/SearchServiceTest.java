package com.scraper.platform.service;

import com.scraper.platform.api.dto.SearchRequest;
import com.scraper.platform.api.dto.SearchResponse;
import com.scraper.platform.crawler.CrawlerFactory;
import com.scraper.platform.crawler.SiteCrawler;
import com.scraper.platform.model.SiteDefinition;
import com.scraper.platform.repository.SiteDefinitionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private CrawlerFactory crawlerFactory;

    @Mock
    private SiteDefinitionRepository siteDefinitionRepository;

    @InjectMocks
    private SearchService searchService;

    private SiteCrawler mockCrawler;
    private SiteDefinition mockSite;

    @BeforeEach
    void setUp() {
        mockCrawler = new SiteCrawler() {
            @Override
            public String getSiteName() {
                return "saramin";
            }

            @Override
            public List<Map<String, String>> search(com.scraper.platform.model.CrawlSiteConfig siteConfig) {
                return List.of(
                    Map.of(
                        "company", "테스트 회사",
                        "position", "Java 개발자",
                        "career", "3~5년",
                        "location", "서울"
                    )
                );
            }
        };

        mockSite = SiteDefinition.builder()
            .id(1L)
            .siteName("saramin")
            .displayName("사람인")
            .isEnabled(true)
            .build();
    }

    @Test
    void search_returnsResults() {
        // Given
        SearchRequest request = new SearchRequest("Java", "3~5년", "서울", List.of("saramin"));

        when(crawlerFactory.getCrawler("saramin")).thenReturn(mockCrawler);
        when(siteDefinitionRepository.findBySiteName("saramin")).thenReturn(Optional.of(mockSite));

        // When
        SearchResponse response = searchService.search(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.total()).isEqualTo(1);
        assertThat(response.jobs()).hasSize(1);
        assertThat(response.jobs().get(0).get("company")).isEqualTo("테스트 회사");
        assertThat(response.siteCounts()).containsEntry("saramin", 1);
    }

    @Test
    void search_withMultipleSites() {
        // Given
        SearchRequest request = new SearchRequest("React", "신입", "전체", List.of("saramin", "wanted"));

        SiteCrawler wantedCrawler = new SiteCrawler() {
            @Override
            public String getSiteName() {
                return "wanted";
            }

            @Override
            public List<Map<String, String>> search(com.scraper.platform.model.CrawlSiteConfig siteConfig) {
                return List.of(
                    Map.of("company", "원티드 회사", "position", "React 개발자")
                );
            }
        };

        SiteDefinition wantedSite = SiteDefinition.builder()
            .id(2L)
            .siteName("wanted")
            .displayName("원티드")
            .isEnabled(true)
            .build();

        when(crawlerFactory.getCrawler("saramin")).thenReturn(mockCrawler);
        when(crawlerFactory.getCrawler("wanted")).thenReturn(wantedCrawler);
        when(siteDefinitionRepository.findBySiteName("saramin")).thenReturn(Optional.of(mockSite));
        when(siteDefinitionRepository.findBySiteName("wanted")).thenReturn(Optional.of(wantedSite));

        // When
        SearchResponse response = searchService.search(request);

        // Then
        assertThat(response.total()).isEqualTo(2);
        assertThat(response.siteCounts()).containsEntry("saramin", 1).containsEntry("wanted", 1);
    }

    @Test
    void search_withNoResults() {
        // Given
        SiteCrawler emptyCrawler = new SiteCrawler() {
            @Override
            public String getSiteName() {
                return "saramin";
            }

            @Override
            public List<Map<String, String>> search(com.scraper.platform.model.CrawlSiteConfig siteConfig) {
                return List.of();
            }
        };

        SearchRequest request = new SearchRequest("비existent", null, null, List.of("saramin"));

        when(crawlerFactory.getCrawler("saramin")).thenReturn(emptyCrawler);
        when(siteDefinitionRepository.findBySiteName("saramin")).thenReturn(Optional.of(mockSite));

        // When
        SearchResponse response = searchService.search(request);

        // Then
        assertThat(response.total()).isEqualTo(0);
        assertThat(response.jobs()).isEmpty();
    }

    @Test
    void search_withFailedSite() {
        // Given
        SiteCrawler failingCrawler = new SiteCrawler() {
            @Override
            public String getSiteName() {
                return "saramin";
            }

            @Override
            public List<Map<String, String>> search(com.scraper.platform.model.CrawlSiteConfig siteConfig) {
                throw new RuntimeException("크롤링 실패");
            }
        };

        SearchRequest request = new SearchRequest("Java", null, null, List.of("saramin"));

        when(crawlerFactory.getCrawler("saramin")).thenReturn(failingCrawler);
        when(siteDefinitionRepository.findBySiteName("saramin")).thenReturn(Optional.of(mockSite));

        // When
        SearchResponse response = searchService.search(request);

        // Then
        assertThat(response.total()).isEqualTo(0);
        assertThat(response.failedSites()).contains("saramin");
    }
}
