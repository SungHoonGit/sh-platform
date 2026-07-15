package com.scraper.platform.crawler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CrawlerFactory {

    private final Map<String, SiteCrawler> crawlerMap;

    public CrawlerFactory(List<SiteCrawler> crawlers) {
        this.crawlerMap = crawlers.stream()
                .collect(Collectors.toMap(SiteCrawler::getSiteName, Function.identity()));
        log.info("Registered crawlers: {}", crawlerMap.keySet());
    }

    public SiteCrawler getCrawler(String siteName) {
        SiteCrawler crawler = crawlerMap.get(siteName);
        if (crawler == null) {
            log.warn("No crawler found for site: {}", siteName);
        }
        return crawler;
    }

    public boolean hasCrawler(String siteName) {
        return crawlerMap.containsKey(siteName);
    }

    public Map<String, SiteCrawler> getAllCrawlers() {
        return crawlerMap;
    }
}
