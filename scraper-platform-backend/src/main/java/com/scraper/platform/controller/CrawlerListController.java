package com.scraper.platform.controller;

import com.scraper.platform.model.CrawlConfig;
import com.scraper.platform.repository.CrawlConfigRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/docs/crawlers")
public class CrawlerListController {

    private final CrawlConfigRepository crawlConfigRepository;

    public CrawlerListController(CrawlConfigRepository crawlConfigRepository) {
        this.crawlConfigRepository = crawlConfigRepository;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getCrawlers() {
        List<CrawlConfig> configs = crawlConfigRepository.findByIsActiveTrue();
        
        List<Map<String, Object>> result = configs.stream()
            .map(config -> Map.<String, Object>of(
                "id", config.getId(),
                "name", config.getName(),
                "localPath", config.getLocalPath() != null ? config.getLocalPath() : "",
                "schedule", config.getSchedule() != null ? config.getSchedule() : ""
            ))
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(result);
    }
}
