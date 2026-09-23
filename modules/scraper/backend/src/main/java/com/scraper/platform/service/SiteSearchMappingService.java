package com.scraper.platform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scraper.platform.api.dto.SearchMappingRequest;
import com.scraper.platform.api.dto.SearchMappingResponse;
import com.scraper.platform.model.SiteDefinition;
import com.scraper.platform.model.SiteSearchMapping;
import com.scraper.platform.repository.SiteDefinitionRepository;
import com.scraper.platform.repository.SiteSearchMappingRepository;
import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 사이트 검색 매핑(value_mapping)을 관리하는 서비스.
 * <p>
 * 029 이후 이 테이블이 크롤러 URL 매핑의 1차 소스이므로, 배포 없이 값을 바꾸기 위한
 * CRUD를 제공한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SiteSearchMappingService {

    private final SiteSearchMappingRepository mappingRepository;
    private final SiteDefinitionRepository siteDefinitionRepository;
    private final ObjectMapper objectMapper;

    /**
     * 전체 매핑을 사이트 표시명·표시 순서 순으로 조회한다.
     *
     * @return 매핑 응답 목록
     */
    public List<SearchMappingResponse> listAll() {
        return mappingRepository.findAllByOrderBySiteDefinition_DisplayNameAscDisplayOrderAscIdAsc()
                .stream().map(SearchMappingResponse::from).toList();
    }

    /**
     * 사이트별 매핑 목록을 표시 순서로 조회한다.
     *
     * @param siteName 사이트 영문명
     * @return 매핑 응답 목록
     */
    public List<SearchMappingResponse> listBySite(String siteName) {
        return mappingRepository.findBySiteDefinitionSiteNameOrderByDisplayOrderAscIdAsc(siteName)
                .stream().map(SearchMappingResponse::from).toList();
    }

    /**
     * 매핑 단건을 조회한다.
     *
     * @param id 매핑 ID
     * @return 매핑 응답
     * @throws BusinessException NOT_FOUND
     */
    public SearchMappingResponse getById(Long id) {
        return SearchMappingResponse.from(findByIdOrThrow(id));
    }

    /**
     * (명령형) 매핑 행을 생성한다.
     *
     * @param request 생성 요청
     * @return 생성된 매핑 응답
     * @throws BusinessException NOT_FOUND, DUPLICATE_NAME, INVALID_INPUT
     */
    @Transactional
    public SearchMappingResponse create(SearchMappingRequest request) {
        SiteDefinition site = siteDefinitionRepository.findById(request.siteDefinitionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (request.standardKey() == null || request.standardKey().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (mappingRepository.existsBySiteDefinitionIdAndStandardKey(site.getId(), request.standardKey())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NAME);
        }
        validateMapping(request.valueType(), request.valueMapping());

        SiteSearchMapping mapping = SiteSearchMapping.builder()
                .siteDefinition(site)
                .standardKey(request.standardKey())
                .urlParamName(request.urlParamName())
                .valueType(request.valueType())
                .valueMapping(request.valueType() == SiteSearchMapping.ValueType.direct ? null : request.valueMapping())
                .isEnabled(request.isEnabled() == null ? true : request.isEnabled())
                .displayOrder(request.displayOrder() == null ? 0 : request.displayOrder())
                .build();
        return SearchMappingResponse.from(mappingRepository.save(mapping));
    }

    /**
     * (명령형) 매핑 행을 수정한다. 사이트·standard_key는 불변.
     *
     * @param id      매핑 ID
     * @param request 수정 요청
     * @return 수정된 매핑 응답
     * @throws BusinessException NOT_FOUND, INVALID_INPUT
     */
    @Transactional
    public SearchMappingResponse update(Long id, SearchMappingRequest request) {
        SiteSearchMapping mapping = findByIdOrThrow(id);
        validateMapping(request.valueType(), request.valueMapping());

        mapping.setUrlParamName(request.urlParamName());
        mapping.setValueType(request.valueType());
        mapping.setValueMapping(request.valueType() == SiteSearchMapping.ValueType.direct ? null : request.valueMapping());
        if (request.isEnabled() != null) {
            mapping.setIsEnabled(request.isEnabled());
        }
        if (request.displayOrder() != null) {
            mapping.setDisplayOrder(request.displayOrder());
        }
        return SearchMappingResponse.from(mappingRepository.save(mapping));
    }

    /**
     * (명령형) 매핑 행을 삭제한다.
     *
     * @param id 매핑 ID
     * @throws BusinessException NOT_FOUND
     */
    @Transactional
    public void delete(Long id) {
        SiteSearchMapping mapping = findByIdOrThrow(id);
        mappingRepository.delete(mapping);
    }

    private SiteSearchMapping findByIdOrThrow(Long id) {
        return mappingRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    /**
     * value_type별 value_mapping 검증: mapped/range/compound는 온전한 JSON 객체 필수
     * (compound는 값이 중첩 객체여야 한다), direct는 null이어야 한다. 실패 시 INVALID_INPUT.
     */
    private void validateMapping(SiteSearchMapping.ValueType valueType, String valueMapping) {
        if (valueType == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (valueType == SiteSearchMapping.ValueType.direct) {
            return;
        }
        if (valueMapping == null || valueMapping.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        try {
            JsonNode node = objectMapper.readTree(valueMapping);
            if (!node.isObject() || node.isEmpty()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }
            if (valueType == SiteSearchMapping.ValueType.compound) {
                for (var it = node.fields(); it.hasNext(); ) {
                    JsonNode entry = it.next().getValue();
                    if (!entry.isObject() || entry.isEmpty()) {
                        throw new BusinessException(ErrorCode.INVALID_INPUT);
                    }
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }
}