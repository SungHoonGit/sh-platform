package com.scraper.platform.repository;

import com.scraper.platform.model.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegionRepository extends JpaRepository<Region, Long> {
    List<Region> findByIsActiveTrueOrderByDisplayOrderAsc();
    List<Region> findTop20ByNameContainingOrderByNameAsc(String name);
    boolean existsByName(String name);
}