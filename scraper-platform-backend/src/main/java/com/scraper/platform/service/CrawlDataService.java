package com.scraper.platform.service;

import com.scraper.platform.model.Category;
import com.scraper.platform.model.CrawlData;
import com.scraper.platform.repository.CrawlDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CrawlDataService {

    private final CrawlDataRepository crawlDataRepository;
    private final CategoryService categoryService;

    public Page<CrawlData> getCrawlDataByCategory(String categorySlug, Pageable pageable) {
        Category category = categoryService.getCategoryBySlug(categorySlug);
        return crawlDataRepository.findByCategoryId(category.getId(), pageable);
    }

    public Page<CrawlData> searchCrawlData(String keyword, Pageable pageable) {
        return crawlDataRepository.searchByKeyword(keyword, pageable);
    }

    public Page<CrawlData> getCrawlDataByCategoryAndFileName(String categorySlug, String fileName, Pageable pageable) {
        Category category = categoryService.getCategoryBySlug(categorySlug);
        return crawlDataRepository.findByCategoryIdAndFileNameContaining(category.getId(), fileName, pageable);
    }

    public long getCountByCategory(String categorySlug) {
        Category category = categoryService.getCategoryBySlug(categorySlug);
        return crawlDataRepository.countByCategoryId(category.getId());
    }
}
