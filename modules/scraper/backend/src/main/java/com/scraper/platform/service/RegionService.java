package com.scraper.platform.service;

import com.scraper.platform.model.Region;
import com.scraper.platform.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegionService {

    private final RegionRepository regionRepository;

    /**
     * 활성 지역 목록을 조회한다 (표시 순서 오름차순).
     *
     * @return 활성 지역 목록
     */
    public List<Region> getActiveRegions() {
        return regionRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
    }

    /**
     * 이름 부분 일치로 지역을 검색한다 (최대 20건).
     *
     * @param query 검색어
     * @return 일치하는 지역 목록
     */
    public List<Region> search(String query) {
        return regionRepository.findTop20ByNameContainingOrderByNameAsc(query);
    }
}