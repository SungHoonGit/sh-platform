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
                .name("Java 시니어 개발자")
                .description("Java 시니어 개발자 크롤링 설정")
                .schedule("0 9 * * *")
                .retentionDays(30)
                .isActive(true)
                .build();
    }

    @Nested
    @DisplayName("getConfigById 메서드")
    class GetConfigById {

        @Test
        @DisplayName("ID로 설정을 조회한다")
        void getConfigById_shouldReturnConfig_whenExists() {
            given(crawlConfigRepository.findById(1L)).willReturn(Optional.of(testConfig));

            var result = crawlConfigService.getConfigById(1L);

            assertNotNull(result);
            assertEquals("Java 시니어 개발자", result.getName());
        }

        @Test
        @DisplayName("존재하지 않는 ID 조회 시 예외 발생")
        void getConfigById_shouldThrow_whenNotExists() {
            given(crawlConfigRepository.findById(999L)).willReturn(Optional.empty());

            assertThrows(RuntimeException.class,
                    () -> crawlConfigService.getConfigById(999L));
        }
    }

    @Nested
    @DisplayName("createConfig 메서드")
    class CreateConfig {

        @Test
        @DisplayName("정상적으로 설정을 생성한다")
        void createConfig_shouldCreate_whenValidInput() {
            CrawlConfig newConfig = CrawlConfig.builder()
                    .name("React 프론트엔드")
                    .build();
            
            given(crawlConfigRepository.existsByName("React 프론트엔드")).willReturn(false);
            when(crawlConfigRepository.save(any(CrawlConfig.class))).thenReturn(newConfig);

            var result = crawlConfigService.createConfig(newConfig);

            assertNotNull(result);
            verify(crawlConfigRepository).save(any(CrawlConfig.class));
        }

        @Test
        @DisplayName("중복 이름으로 생성 시 예외 발생")
        void createConfig_shouldThrow_whenDuplicateName() {
            given(crawlConfigRepository.existsByName("Java 시니어 개발자")).willReturn(true);

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
            given(crawlConfigRepository.findById(1L)).willReturn(Optional.of(testConfig));
            when(crawlConfigRepository.save(any(CrawlConfig.class))).thenReturn(testConfig);

            CrawlConfig updatedConfig = CrawlConfig.builder()
                    .name("Java 시니어 개발자 (수정)")
                    .schedule("0 10 * * *")
                    .build();

            var result = crawlConfigService.updateConfig(1L, updatedConfig);

            assertNotNull(result);
            verify(crawlConfigRepository).save(any(CrawlConfig.class));
        }
    }
}
