package com.scraper.platform.service;

import com.scraper.platform.model.CrawlData;
import com.scraper.platform.repository.CrawlDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CrawlDataService {

    private final CrawlDataRepository crawlDataRepository;

    public Page<CrawlData> getCrawlDataByCategory(String category, Pageable pageable) {
        return crawlDataRepository.findByCategory(category, pageable);
    }

    public Page<CrawlData> searchCrawlData(String keyword, Pageable pageable) {
        return crawlDataRepository.searchByKeyword(keyword, pageable);
    }

    public Page<CrawlData> advancedSearch(
            String keyword,
            String site,
            String category,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {
        
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) : null;
        
        return crawlDataRepository.advancedSearch(keyword, site, category, start, end, pageable);
    }

    public Page<CrawlData> getCrawlDataByCategoryAndFileName(String category, String fileName, Pageable pageable) {
        return crawlDataRepository.findByCategoryAndFileNameContaining(category, fileName, pageable);
    }

    public long getCountByCategory(String category) {
        return crawlDataRepository.countByCategory(category);
    }
}
