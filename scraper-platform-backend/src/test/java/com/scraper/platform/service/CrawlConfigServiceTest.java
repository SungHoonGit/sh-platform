package com.scraper.platform.service;

import com.scraper.platform.model.CrawlConfig;
import com.scraper.platform.repository.CrawlConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CrawlConfigService 테스트")
class CrawlConfigServiceTest {

    @Mock
    private CrawlConfigRepository crawlConfigRepository;

    @InjectMocks
    private CrawlConfigService crawlConfigService;

    private CrawlConfig testConfig;

    @BeforeEach
    void setUp() {
        testConfig = CrawlConfig.builder()
                .id(1L)
                .category("java")
                .query("Java Spring 백엔드 개발자")
                .careerLevel("경력")
                .careerFrom(3)
                .careerTo(5)
                .sites("[\"saramin\",\"wanted\"]")
                .isActive(true)
                .build();
    }

    @Nested
    @DisplayName("getConfigBySlug 메서드")
    class GetConfigByCategory {

        @Test
        @DisplayName("카테고리로 설정을 조회한다")
        void getConfigByCategory_shouldReturnConfig_whenExists() {
            given(crawlConfigRepository.findByCategory("java")).willReturn(Optional.of(testConfig));

            var result = crawlConfigService.getConfigByCategory("java");

            assertNotNull(result);
            assertEquals("java", result.getCategory());
        }

        @Test
        @DisplayName("존재하지 않는 카테고리 조회 시 예외 발생")
        void getConfigByCategory_shouldThrow_whenNotExists() {
            given(crawlConfigRepository.findByCategory("nonexistent")).willReturn(Optional.empty());

            assertThrows(RuntimeException.class,
                    () -> crawlConfigService.getConfigByCategory("nonexistent"));
        }
    }

    @Nested
    @DisplayName("createConfig 메서드")
    class CreateConfig {

        @Test
        @DisplayName("정상적으로 설정을 생성한다")
        void createConfig_shouldCreate_whenValidInput() {
            CrawlConfig newConfig = CrawlConfig.builder()
                    .category("react")
                    .query("React 프론트엔드")
                    .build();
            
            given(crawlConfigRepository.existsByCategory("react")).willReturn(false);
            when(crawlConfigRepository.save(any(CrawlConfig.class))).thenReturn(newConfig);

            var result = crawlConfigService.createConfig(newConfig);

            assertNotNull(result);
            verify(crawlConfigRepository).save(any(CrawlConfig.class));
        }

        @Test
        @DisplayName("중복 카테고리로 생성 시 예외 발생")
        void createConfig_shouldThrow_whenDuplicateCategory() {
            given(crawlConfigRepository.existsByCategory("java")).willReturn(true);

            assertThrows(RuntimeException.class,
                    () -> crawlConfigService.createConfig(testConfig));
        }
    }

    @Nested
    @DisplayName("updateConfig 메서드")
    class UpdateConfig {

        @Test
        @DisplayName("정상적으로 설정을 수정한다")
        void updateConfig_shouldUpdate_whenExists() {
            given(crawlConfigRepository.findByCategory("java")).willReturn(Optional.of(testConfig));
            when(crawlConfigRepository.save(any(CrawlConfig.class))).thenReturn(testConfig);

            CrawlConfig updatedConfig = CrawlConfig.builder()
                    .query("Java Spring 백엔드 개발자 서울")
                    .careerFrom(5)
                    .careerTo(8)
                    .build();

            var result = crawlConfigService.updateConfig("java", updatedConfig);

            assertNotNull(result);
            verify(crawlConfigRepository).save(any(CrawlConfig.class));
        }
    }
}
