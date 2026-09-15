package com.scraper.platform.service;

import com.scraper.platform.crawler.CrawlerFactory;
import com.scraper.platform.model.SiteDefinition;
import com.scraper.platform.repository.SiteDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SiteCrawlerRegistrationValidator implements ApplicationRunner {

    private final CrawlerFactory crawlerFactory;
    private final SiteDefinitionRepository siteDefinitionRepository;

    @Override
    public void run(ApplicationArguments args) {
        Set<String> crawlerSites = crawlerFactory.getAllCrawlers().keySet();
        Set<String> dbSites = siteDefinitionRepository.findAll().stream()
                .map(SiteDefinition::getSiteName)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        for (String site : crawlerSites) {
            if (!dbSites.contains(site)) {
                log.warn("[site-master] 크롤러 {} 이(가) site_definition에 없음 — DB 시드 필요", site);
            }
        }
        for (String site : dbSites) {
            if (!crawlerSites.contains(site)) {
                log.warn("[site-master] site_definition의 {} 은(는) 등록된 크롤러 없음", site);
            }
        }
        log.info("[site-master] 크롤러 등록 {}개, site_definition {}개 일치 검증 완료",
                crawlerSites.size(), dbSites.size());
    }
}