package com.scraper.platform.service;

import com.scraper.platform.model.Region;
import com.scraper.platform.repository.RegionRepository;
import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
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

    /**
     * (명령형) 지역 마스터를 생성한다.
     *
     * @param request 지역명, 표시 순서, 활성 여부
     * @return 생성된 지역
     * @throws BusinessException DUPLICATE_NAME 이름 중복
     */
    @Transactional
    public Region create(Region request) {
        if (regionRepository.existsByName(request.getName())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NAME);
        }
        if (request.getDisplayOrder() == null) {
            request.setDisplayOrder(0);
        }
        if (request.getIsActive() == null) {
            request.setIsActive(true);
        }
        return regionRepository.save(request);
    }

    /**
     * (명령형) 지역 마스터를 수정한다.
     *
     * @param id 지역 ID
     * @param request 수정할 지역명, 표시 순서, 활성 여부
     * @return 수정된 지역
     * @throws BusinessException NOT_FOUND, DUPLICATE_NAME
     */
    @Transactional
    public Region update(Long id, Region request) {
        Region existing = regionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!existing.getName().equals(request.getName()) && regionRepository.existsByName(request.getName())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NAME);
        }
        existing.setName(request.getName());
        if (request.getDisplayOrder() != null) {
            existing.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getIsActive() != null) {
            existing.setIsActive(request.getIsActive());
        }
        return regionRepository.save(existing);
    }

    /**
     * (명령형) 지역 마스터를 삭제한다.
     *
     * @param id 지역 ID
     * @throws BusinessException NOT_FOUND
     */
    @Transactional
    public void delete(Long id) {
        if (!regionRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        regionRepository.deleteById(id);
    }
}