package com.scraper.platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scraper.platform.api.dto.SearchMappingRequest;
import com.scraper.platform.api.dto.SearchMappingResponse;
import com.scraper.platform.model.SiteDefinition;
import com.scraper.platform.model.SiteSearchMapping;
import com.scraper.platform.repository.SiteDefinitionRepository;
import com.scraper.platform.repository.SiteSearchMappingRepository;
import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SiteSearchMappingService 테스트")
class SiteSearchMappingServiceTest {

    @Mock
    private SiteSearchMappingRepository mappingRepository;

    @Mock
    private SiteDefinitionRepository siteDefinitionRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private SiteSearchMappingService searchMappingService;

    private SiteDefinition saraminSite;

    @BeforeEach
    void setUp() {
        saraminSite = SiteDefinition.builder()
                .id(1L)
                .siteName("saramin")
                .displayName("사람인")
                .build();
    }

    @Nested
    @DisplayName("조회 메서드")
    class Query {

        @Test
        @DisplayName("전체 매핑을 사이트명·표시명 포함 응답으로 조회한다")
        void 전체_조회() {
            // given
            SiteSearchMapping mapping = mapping(saraminSite, "location", "loc_cd",
                    SiteSearchMapping.ValueType.mapped, "{\"서울\":\"101000\"}", 3);
            given(mappingRepository.findAllByOrderBySiteDefinition_DisplayNameAscDisplayOrderAscIdAsc())
                    .willReturn(List.of(mapping));

            // when
            List<SearchMappingResponse> result = searchMappingService.listAll();

            // then
            assertEquals(1, result.size());
            SearchMappingResponse r = result.get(0);
            assertEquals("saramin", r.siteName());
            assertEquals("사람인", r.siteDisplayName());
            assertEquals("location", r.standardKey());
            assertEquals("{\"서울\":\"101000\"}", r.valueMapping());
        }

        @Test
        @DisplayName("사이트별 매핑을 조회한다")
        void 사이트별_조회() {
            // given
            SiteSearchMapping mapping = mapping(saraminSite, "keyword", "stext",
                    SiteSearchMapping.ValueType.direct, null, 1);
            given(mappingRepository.findBySiteDefinitionSiteNameOrderByDisplayOrderAscIdAsc("saramin"))
                    .willReturn(List.of(mapping));

            // when
            List<SearchMappingResponse> result = searchMappingService.listBySite("saramin");

            // then
            assertEquals(1, result.size());
            assertEquals("stext", result.get(0).urlParamName());
        }

        @Test
        @DisplayName("단건 조회 시 없으면 NOT_FOUND 예외를 던진다")
        void 없는ID_예외() {
            given(mappingRepository.findById(999L)).willReturn(Optional.empty());
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> searchMappingService.getById(999L));
            assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("create 메서드")
    class Create {

        @Test
        @DisplayName("mapped 타입으로 매핑을 생성한다")
        void 생성() {
            // given
            SearchMappingRequest request = new SearchMappingRequest(
                    1L, "location", "loc_cd", SiteSearchMapping.ValueType.mapped,
                    "{\"서울\":\"101000\",\"제주\":\"500000\"}", true, 3);
            given(siteDefinitionRepository.findById(1L)).willReturn(Optional.of(saraminSite));
            given(mappingRepository.existsBySiteDefinitionIdAndStandardKey(1L, "location")).willReturn(false);
            given(mappingRepository.save(org.mockito.ArgumentMatchers.any())).willAnswer(inv -> {
                SiteSearchMapping m = inv.getArgument(0);
                m.setId(20L);
                return m;
            });

            // when
            SearchMappingResponse result = searchMappingService.create(request);

            // then
            assertEquals(20L, result.id());
            assertEquals(3, result.displayOrder());
            assertTrue(result.isEnabled());
            assertEquals("{\"서울\":\"101000\",\"제주\":\"500000\"}", result.valueMapping());
        }

        @Test
        @DisplayName("중복 (사이트, standard_key)면 DUPLICATE_NAME 예외를 던진다")
        void 중복_예외() {
            SearchMappingRequest request = new SearchMappingRequest(
                    1L, "location", "loc_cd", SiteSearchMapping.ValueType.mapped, "{\"서울\":\"101000\"}", true, 3);
            given(siteDefinitionRepository.findById(1L)).willReturn(Optional.of(saraminSite));
            given(mappingRepository.existsBySiteDefinitionIdAndStandardKey(1L, "location")).willReturn(true);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> searchMappingService.create(request));
            assertEquals(ErrorCode.DUPLICATE_NAME, ex.getErrorCode());
        }

        @Test
        @DisplayName("없는 사이트면 NOT_FOUND 예외를 던진다")
        void 없는사이트_예외() {
            SearchMappingRequest request = new SearchMappingRequest(
                    999L, "location", "loc_cd", SiteSearchMapping.ValueType.mapped, "{\"서울\":\"101000\"}", true, 3);
            given(siteDefinitionRepository.findById(999L)).willReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> searchMappingService.create(request));
            assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("mapped인데 value_mapping이 JSON 객체가 아니면 INVALID_INPUT")
        void 잘못된JSON_예외() {
            SearchMappingRequest request = new SearchMappingRequest(
                    1L, "location", "loc_cd", SiteSearchMapping.ValueType.mapped, "[1,2,3]", true, 3);
            given(siteDefinitionRepository.findById(1L)).willReturn(Optional.of(saraminSite));
            given(mappingRepository.existsBySiteDefinitionIdAndStandardKey(1L, "location")).willReturn(false);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> searchMappingService.create(request));
            assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
        }

        @Test
        @DisplayName("direct 타입은 value_mapping 없이 생성된다")
        void direct_생성() {
            SearchMappingRequest request = new SearchMappingRequest(
                    1L, "keyword", "stext", SiteSearchMapping.ValueType.direct, "{\"불필요\":\"값\"}", true, 1);
            given(siteDefinitionRepository.findById(1L)).willReturn(Optional.of(saraminSite));
            given(mappingRepository.existsBySiteDefinitionIdAndStandardKey(1L, "keyword")).willReturn(false);
            given(mappingRepository.save(org.mockito.ArgumentMatchers.any())).willAnswer(inv -> {
                SiteSearchMapping m = inv.getArgument(0);
                m.setId(21L);
                return m;
            });

            SearchMappingResponse result = searchMappingService.create(request);

            assertEquals(21L, result.id());
            assertNull(result.valueMapping());
        }

        @Test
        @DisplayName("compound 타입은 중첩 객체 JSON으로 생성된다")
        void compound_생성() {
            SearchMappingRequest request = new SearchMappingRequest(
                    1L, "career", "exp_cd", SiteSearchMapping.ValueType.compound,
                    "{\"1~3년\":{\"exp_cd\":\"2\",\"exp_min\":\"1\",\"exp_max\":\"3\"}}", true, 2);
            given(siteDefinitionRepository.findById(1L)).willReturn(Optional.of(saraminSite));
            given(mappingRepository.existsBySiteDefinitionIdAndStandardKey(1L, "career")).willReturn(false);
            given(mappingRepository.save(org.mockito.ArgumentMatchers.any())).willAnswer(inv -> {
                SiteSearchMapping m = inv.getArgument(0);
                m.setId(22L);
                return m;
            });

            SearchMappingResponse result = searchMappingService.create(request);

            assertEquals(SiteSearchMapping.ValueType.compound, result.valueType());
            assertEquals("{\"1~3년\":{\"exp_cd\":\"2\",\"exp_min\":\"1\",\"exp_max\":\"3\"}}", result.valueMapping());
        }

        @Test
        @DisplayName("compound인데 값이 중첩 객체가 아니면 INVALID_INPUT")
        void compound_잘못된구조_예외() {
            SearchMappingRequest request = new SearchMappingRequest(
                    1L, "career", "exp_cd", SiteSearchMapping.ValueType.compound,
                    "{\"1~3년\":\"2\"}", true, 2);
            given(siteDefinitionRepository.findById(1L)).willReturn(Optional.of(saraminSite));
            given(mappingRepository.existsBySiteDefinitionIdAndStandardKey(1L, "career")).willReturn(false);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> searchMappingService.create(request));
            assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("update 메서드")
    class Update {

        @Test
        @DisplayName("value_mapping과 표시 순서를 수정한다")
        void 수정() {
            // given
            SiteSearchMapping existing = mapping(saraminSite, "location", "loc_cd",
                    SiteSearchMapping.ValueType.mapped, "{\"서울\":\"101000\"}", 3);
            existing.setId(1L);
            SearchMappingRequest request = new SearchMappingRequest(
                    null, null, "loc_cd", SiteSearchMapping.ValueType.mapped,
                    "{\"서울\":\"101000\",\"세종\":\"360000\"}", false, 4);
            given(mappingRepository.findById(1L)).willReturn(Optional.of(existing));
            given(mappingRepository.save(existing)).willReturn(existing);

            // when
            SearchMappingResponse result = searchMappingService.update(1L, request);

            // then
            assertEquals("{\"서울\":\"101000\",\"세종\":\"360000\"}", result.valueMapping());
            assertEquals(4, result.displayOrder());
            assertFalse(result.isEnabled());
        }

        @Test
        @DisplayName("없는 ID면 NOT_FOUND 예외를 던진다")
        void 없는ID_예외() {
            SearchMappingRequest request = new SearchMappingRequest(
                    null, null, "loc_cd", SiteSearchMapping.ValueType.mapped, "{\"서울\":\"101000\"}", true, 3);
            given(mappingRepository.findById(999L)).willReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> searchMappingService.update(999L, request));
            assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("delete 메서드")
    class Delete {

        @Test
        @DisplayName("매핑을 삭제한다")
        void 삭제() {
            SiteSearchMapping existing = mapping(saraminSite, "location", "loc_cd",
                    SiteSearchMapping.ValueType.mapped, "{\"서울\":\"101000\"}", 3);
            given(mappingRepository.findById(1L)).willReturn(Optional.of(existing));

            searchMappingService.delete(1L);

            verify(mappingRepository).delete(existing);
        }

        @Test
        @DisplayName("없는 ID면 NOT_FOUND 예외를 던진다")
        void 없는ID_예외() {
            given(mappingRepository.findById(999L)).willReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> searchMappingService.delete(999L));
            assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        }
    }

    private SiteSearchMapping mapping(SiteDefinition site, String standardKey, String urlParamName,
                                      SiteSearchMapping.ValueType valueType, String valueMapping, int order) {
        return SiteSearchMapping.builder()
                .siteDefinition(site)
                .standardKey(standardKey)
                .urlParamName(urlParamName)
                .valueType(valueType)
                .valueMapping(valueMapping)
                .isEnabled(true)
                .displayOrder(order)
                .build();
    }
}