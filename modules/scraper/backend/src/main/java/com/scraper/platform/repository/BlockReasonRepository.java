package com.scraper.platform.repository;

import com.scraper.platform.model.BlockReason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 회사 차단 카테고리 마스터 저장소.
 */
public interface BlockReasonRepository extends JpaRepository<BlockReason, Long> {

    List<BlockReason> findTop20ByNameContainingAndActiveTrueOrderBySortOrderAsc(String name);

    List<BlockReason> findAllByActiveTrueOrderBySortOrderAsc();
}
