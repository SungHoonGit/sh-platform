package com.scraper.platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scraper.platform.model.SiteDefinition;
import com.scraper.platform.model.SiteSearchMapping;
import com.scraper.platform.repository.SiteSearchMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("SiteSearchMapper 테스트")
class SiteSearchMapperTest {

    @Mock
    private SiteSearchMappingRepository mappingRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private SiteSearchMapper siteSearchMapper;

    private SiteDefinition saraminSite;
    private SiteDefinition wantedSite;

    @BeforeEach
    void setUp() {
        saraminSite = SiteDefinition.builder()
                .id(1L)
                .siteName("saramin")
                .displayName("사람인")
                .build();

        wantedSite = SiteDefinition.builder()
                .id(3L)
                .siteName("wanted")
                .displayName("원티드")
                .build();
    }

    @Nested
    @DisplayName("사람인 매핑")
    class SaraminMapping {

        @Test
        @DisplayName("키워드를 stext로 변환한다")
        void 키워드_변환() {
            // given
            List<SiteSearchMapping> mappings = List.of(
                mapping("keyword", "stext", SiteSearchMapping.ValueType.direct, null)
            );
            given(mappingRepository.findBySiteDefinition_SiteNameAndIsEnabledTrueOrderByDisplayOrder("saramin"))
                    .willReturn(mappings);

            // when
            Map<String, String> result = siteSearchMapper.toSiteParams("saramin", "{\"keyword\":\"React\"}");

            // then
            assertEquals("React", result.get("stext"));
        }

        @Test
        @DisplayName("경력 3~5년을 career_level 5로 변환한다")
        void 경력_3_5년_변환() {
            // given
            List<SiteSearchMapping> mappings = List.of(
                mapping("career", "career_level", SiteSearchMapping.ValueType.mapped,
                        "{\"신입\":\"1\",\"경력\":\"2\",\"1~3년\":\"3\",\"3~5년\":\"5\",\"5~10년\":\"8\",\"10년이상\":\"12\"}")
            );
            given(mappingRepository.findBySiteDefinition_SiteNameAndIsEnabledTrueOrderByDisplayOrder("saramin"))
                    .willReturn(mappings);

            // when
            Map<String, String> result = siteSearchMapper.toSiteParams("saramin", "{\"career\":\"3~5년\"}");

            // then
            assertEquals("5", result.get("career_level"));
        }

        @Test
        @DisplayName("지역 서울을 loc_cd 101000으로 변환한다")
        void 지역_서울_변환() {
            // given
            List<SiteSearchMapping> mappings = List.of(
                mapping("location", "loc_cd", SiteSearchMapping.ValueType.mapped,
                        "{\"서울\":\"101000\",\"경기\":\"102000\"}")
            );
            given(mappingRepository.findBySiteDefinition_SiteNameAndIsEnabledTrueOrderByDisplayOrder("saramin"))
                    .willReturn(mappings);

            // when
            Map<String, String> result = siteSearchMapper.toSiteParams("saramin", "{\"location\":\"서울\"}");

            // then
            assertEquals("101000", result.get("loc_cd"));
        }

        @Test
        @DisplayName("복수 지역을 콤마 구분 코드로 변환한다")
        void 복수지역_변환() {
            // given
            List<SiteSearchMapping> mappings = List.of(
                mapping("location", "loc_cd", SiteSearchMapping.ValueType.mapped,
                        "{\"서울\":\"101000\",\"경기\":\"102000\",\"인천\":\"230000\"}")
            );
            given(mappingRepository.findBySiteDefinition_SiteNameAndIsEnabledTrueOrderByDisplayOrder("saramin"))
                    .willReturn(mappings);

            // when
            Map<String, String> result = siteSearchMapper.toSiteParams("saramin", "{\"location\":\"서울,경기\"}");

            // then
            assertEquals("101000,102000", result.get("loc_cd"));
        }

        @Test
        @DisplayName("매핑에 없는 지역이 포함되면 결과에서 제외된다")
        void 복수지역_부분미매핑_제외() {
            // given
            List<SiteSearchMapping> mappings = List.of(
                mapping("location", "loc_cd", SiteSearchMapping.ValueType.mapped,
                        "{\"서울\":\"101000\"}")
            );
            given(mappingRepository.findBySiteDefinition_SiteNameAndIsEnabledTrueOrderByDisplayOrder("saramin"))
                    .willReturn(mappings);

            // when
            Map<String, String> result = siteSearchMapper.toSiteParams("saramin", "{\"location\":\"서울,경기\"}");

            // then
            assertNull(result.get("loc_cd"));
        }

        @Test
        @DisplayName("복수 파라미터를 한번에 변환한다")
        void 복수_파라미터_변환() {
            // given
            List<SiteSearchMapping> mappings = List.of(
                mapping("keyword", "stext", SiteSearchMapping.ValueType.direct, null),
                mapping("career", "career_level", SiteSearchMapping.ValueType.mapped,
                        "{\"3~5년\":\"5\"}"),
                mapping("location", "loc_cd", SiteSearchMapping.ValueType.mapped,
                        "{\"서울\":\"101000\"}")
            );
            given(mappingRepository.findBySiteDefinition_SiteNameAndIsEnabledTrueOrderByDisplayOrder("saramin"))
                    .willReturn(mappings);

            // when
            Map<String, String> result = siteSearchMapper.toSiteParams("saramin",
                    "{\"keyword\":\"Java\",\"career\":\"3~5년\",\"location\":\"서울\"}");

            // then
            assertEquals("Java", result.get("stext"));
            assertEquals("5", result.get("career_level"));
            assertEquals("101000", result.get("loc_cd"));
        }

        @Test
        @DisplayName("매핑에 없는 값은 결과에 포함되지 않는다")
        void 매핑없는값_제외() {
            // given
            List<SiteSearchMapping> mappings = List.of(
                mapping("career", "career_level", SiteSearchMapping.ValueType.mapped,
                        "{\"신입\":\"1\",\"경력\":\"2\"}")
            );
            given(mappingRepository.findBySiteDefinition_SiteNameAndIsEnabledTrueOrderByDisplayOrder("saramin"))
                    .willReturn(mappings);

            // when
            Map<String, String> result = siteSearchMapper.toSiteParams("saramin", "{\"career\":\"5~10년\"}");

            // then
            assertNull(result.get("career_level"));
        }
    }

    @Nested
    @DisplayName("원티드 매핑")
    class WantedMapping {

        @Test
        @DisplayName("경력 3~5년을 years 3으로 변환한다")
        void 경력_3_5년_변환() {
            // given
            List<SiteSearchMapping> mappings = List.of(
                mapping("career", "years", SiteSearchMapping.ValueType.mapped,
                        "{\"신입\":\"0\",\"1~3년\":\"1\",\"3~5년\":\"3\",\"5~10년\":\"5\"}")
            );
            given(mappingRepository.findBySiteDefinition_SiteNameAndIsEnabledTrueOrderByDisplayOrder("wanted"))
                    .willReturn(mappings);

            // when
            Map<String, String> result = siteSearchMapper.toSiteParams("wanted", "{\"career\":\"3~5년\"}");

            // then
            assertEquals("3", result.get("years"));
        }

        @Test
        @DisplayName("지역 서울을 locations seoul로 변환한다")
        void 지역_서울_변환() {
            // given
            List<SiteSearchMapping> mappings = List.of(
                mapping("location", "locations", SiteSearchMapping.ValueType.mapped,
                        "{\"서울\":\"seoul\",\"경기\":\"gyeonggi\"}")
            );
            given(mappingRepository.findBySiteDefinition_SiteNameAndIsEnabledTrueOrderByDisplayOrder("wanted"))
                    .willReturn(mappings);

            // when
            Map<String, String> result = siteSearchMapper.toSiteParams("wanted", "{\"location\":\"서울\"}");

            // then
            assertEquals("seoul", result.get("locations"));
        }
    }

    @Nested
    @DisplayName("빈 파라미터 처리")
    class EmptyParamHandling {

        @Test
        @DisplayName("빈 문자열 파라미터는 빈 맵을 반환한다")
        void 빈문자열_빈맵() {
            // when
            Map<String, String> result = siteSearchMapper.toSiteParams("saramin", "");

            // then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("null 파라미터는 빈 맵을 반환한다")
        void null_빈맵() {
            // when
            Map<String, String> result = siteSearchMapper.toSiteParams("saramin", (String) null);

            // then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("존재하지 않는 사이트는 매핑 없이 빈 맵 반환")
        void 없는사이트_빈맵() {
            // given
            given(mappingRepository.findBySiteDefinition_SiteNameAndIsEnabledTrueOrderByDisplayOrder("unknown"))
                    .willReturn(List.of());

            // when
            Map<String, String> result = siteSearchMapper.toSiteParams("unknown", "{\"keyword\":\"test\"}");

            // then
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Map 입력 테스트")
    class MapInputTest {

        @Test
        @DisplayName("Map<String, String> 입력으로도 변환된다")
        void map입력_변환() {
            // given
            List<SiteSearchMapping> mappings = List.of(
                mapping("keyword", "stext", SiteSearchMapping.ValueType.direct, null),
                mapping("career", "career_level", SiteSearchMapping.ValueType.mapped,
                        "{\"3~5년\":\"5\"}")
            );
            given(mappingRepository.findBySiteDefinition_SiteNameAndIsEnabledTrueOrderByDisplayOrder("saramin"))
                    .willReturn(mappings);

            // when
            Map<String, String> result = siteSearchMapper.toSiteParams("saramin",
                    Map.of("keyword", "Java", "career", "3~5년"));

            // then
            assertEquals("Java", result.get("stext"));
            assertEquals("5", result.get("career_level"));
        }
    }

    private SiteSearchMapping mapping(String standardKey, String urlParamName,
                                       SiteSearchMapping.ValueType valueType, String valueMapping) {
        return SiteSearchMapping.builder()
                .standardKey(standardKey)
                .urlParamName(urlParamName)
                .valueType(valueType)
                .valueMapping(valueMapping)
                .isEnabled(true)
                .build();
    }
}
