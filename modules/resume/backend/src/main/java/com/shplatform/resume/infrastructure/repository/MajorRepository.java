package com.shplatform.resume.infrastructure.repository;

import com.shplatform.resume.infrastructure.entity.MajorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MajorRepository extends JpaRepository<MajorEntity, Long> {

    List<MajorEntity> findTop20ByNameContainingOrderByNameAsc(String name);
}
