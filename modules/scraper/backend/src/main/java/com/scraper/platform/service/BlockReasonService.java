package com.scraper.platform.service;

import com.scraper.platform.model.BlockReason;
import com.scraper.platform.repository.BlockReasonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 회사 차단 사유 마스터 조회. 차단 사유 등록 시 자동완성/선택용으로 사용된다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlockReasonService {

    private final BlockReasonRepository repository;

    /**
     * (질의형) 활성화된 차단 카테고리를 이름으로 검색한다.
     *
     * @param keyword 검색어 (카테고리명 포함)
     * @return 일치하는 카테고리 목록 (최대 20건, 정렬 순서 오름차순). 빈 키워드면 빈 목록.
     */
    public List<BlockReason> search(String keyword) {
        String q = StringUtils.hasText(keyword) ? keyword.trim() : "";
        if (!StringUtils.hasText(q)) {
            return List.of();
        }
        return repository.findTop20ByNameContainingAndActiveTrueOrderBySortOrderAsc(q);
    }

    /**
     * 활성화된 전체 차단 카테고리 목록(회사유형 + 사유). 다중 선택 UI 초기 로드용.
     *
     * @return 활성 카테고리 전체 (정렬 순서 오름차순)
     */
    public List<BlockReason> listAll() {
        return repository.findAllByActiveTrueOrderBySortOrderAsc();
    }
}
