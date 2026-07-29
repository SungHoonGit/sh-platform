package com.scraper.platform.service;

import com.scraper.platform.model.SiteDefinition;
import com.scraper.platform.model.SiteSearchMapping;
import com.scraper.platform.repository.SiteDefinitionRepository;
import com.scraper.platform.repository.SiteSearchMappingRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SiteSearchMappingInitializer {

    private final SiteDefinitionRepository siteDefinitionRepository;
    private final SiteSearchMappingRepository mappingRepository;

    @PostConstruct
    @Transactional
    public void init() {
        if (mappingRepository.count() > 0) {
            log.info("SiteSearchMapping data already exists (count={}), skipping initialization", mappingRepository.count());
            return;
        }

        log.info("Initializing SiteSearchMapping data...");
        List<SiteSearchMapping> mappings = new ArrayList<>();

        for (SiteDefinition site : siteDefinitionRepository.findByIsEnabledTrue()) {
            String name = site.getSiteName();
            log.info("Adding mapping data for site: {} ({})", name, site.getDisplayName());
            mappings.addAll(buildMappings(site));
        }

        if (!mappings.isEmpty()) {
            mappingRepository.saveAll(mappings);
            log.info("Inserted {} SiteSearchMapping rows", mappings.size());
        } else {
            log.warn("No enabled sites found in site_definition table. Skipping mapping initialization.");
        }
    }

    private List<SiteSearchMapping> buildMappings(SiteDefinition site) {
        return switch (site.getSiteName()) {
            case "saramin" -> List.of(
                    mapping(site, "keyword", "stext", SiteSearchMapping.ValueType.direct, null, 1),
                    mapping(site, "career", "career_level", SiteSearchMapping.ValueType.mapped,
                            "{\"신입\":\"1\",\"경력\":\"2\",\"1~3년\":\"3\",\"3~5년\":\"5\",\"5~10년\":\"8\",\"10년이상\":\"12\"}", 2),
                    mapping(site, "location", "loc_cd", SiteSearchMapping.ValueType.mapped,
                            "{\"서울\":\"101000\",\"경기\":\"102000\",\"인천\":\"230000\",\"부산\":\"260000\",\"대구\":\"270000\",\"대전\":\"300000\",\"광주\":\"290000\",\"세종\":\"360000\",\"강원\":\"420000\",\"제주\":\"500000\",\"충남\":\"440000\",\"충북\":\"430000\",\"전남\":\"460000\",\"전북\":\"450000\",\"경남\":\"480000\",\"경북\":\"470000\"}",
                            3),
                    mapping(site, "job_type", "cat_kewd", SiteSearchMapping.ValueType.mapped,
                            "{\"개발\":\"235\",\"기획\":\"200\",\"디자인\":\"260\",\"마케팅\":\"300\",\"영업\":\"400\",\"연구개발\":\"350\"}", 4),
                    mapping(site, "employment", "job_type", SiteSearchMapping.ValueType.mapped,
                            "{\"정규직\":\"1\",\"계약직\":\"2\",\"인턴\":\"3\",\"프리랜서\":\"4\",\"파견직\":\"5\"}", 5)
            );
            case "jobkorea" -> List.of(
                    mapping(site, "keyword", "stext", SiteSearchMapping.ValueType.direct, null, 1),
                    mapping(site, "career", "careerType", SiteSearchMapping.ValueType.mapped,
                            "{\"신입\":\"new\",\"경력\":\"career\",\"1~3년\":\"career\",\"3~5년\":\"career\",\"5~10년\":\"career\",\"10년이상\":\"career\"}", 2),
                    mapping(site, "location", "local", SiteSearchMapping.ValueType.mapped,
                            "{\"서울\":\"I000\",\"경기\":\"I100\",\"인천\":\"I200\",\"부산\":\"I300\",\"대구\":\"I400\",\"대전\":\"I500\",\"광주\":\"I600\",\"세종\":\"I700\",\"강원\":\"I800\",\"제주\":\"I900\",\"충남\":\"I110\",\"충북\":\"I120\",\"전남\":\"I130\",\"전북\":\"I140\",\"경남\":\"I150\",\"경북\":\"I160\"}",
                            3),
                    mapping(site, "job_type", "dutyCtgr", SiteSearchMapping.ValueType.mapped,
                            "{\"서버/백엔드\":\"1003101\",\"프론트엔드\":\"1003102\",\"풀스택\":\"1003103\",\"모바일\":\"1003104\",\"인프라/DBA\":\"1003105\",\"데이터/AI\":\"1003106\",\"보안\":\"1003107\",\"게임\":\"1003108\",\"기타\":\"1003199\"}",
                            4)
            );
            case "wanted" -> List.of(
                    mapping(site, "keyword", "query", SiteSearchMapping.ValueType.direct, null, 1),
                    mapping(site, "career", "years", SiteSearchMapping.ValueType.mapped,
                            "{\"신입\":\"0\",\"1~3년\":\"1\",\"3~5년\":\"3\",\"5~10년\":\"5\",\"10년이상\":\"10\"}", 2),
                    mapping(site, "location", "locations", SiteSearchMapping.ValueType.mapped,
                            "{\"서울\":\"seoul\",\"경기\":\"gyeonggi\",\"인천\":\"incheon\",\"부산\":\"busan\",\"대구\":\"daegu\",\"대전\":\"daejeon\",\"광주\":\"gwangju\",\"세종\":\"sejong\",\"강원\":\"gangwon\",\"제주\":\"jeju\"}",
                            3),
                    mapping(site, "job_type", "job_group_ids", SiteSearchMapping.ValueType.mapped,
                            "{\"백엔드\":\"518\",\"프론트엔드\":\"660\",\"모바일\":\"519\",\"데이터\":\"777\",\"인프라\":\"669\"}", 4)
            );
            case "remember" -> List.of(
                    mapping(site, "keyword", "query", SiteSearchMapping.ValueType.direct, null, 1),
                    mapping(site, "career", "min_experience", SiteSearchMapping.ValueType.mapped,
                            "{\"신입\":\"0\",\"1~3년\":\"1\",\"3~5년\":\"3\",\"5~10년\":\"5\",\"10년이상\":\"10\"}", 2),
                    mapping(site, "location", "sido", SiteSearchMapping.ValueType.direct, null, 3)
            );
            default -> List.of();
        };
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
