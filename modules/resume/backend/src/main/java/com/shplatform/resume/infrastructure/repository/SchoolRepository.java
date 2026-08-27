package com.shplatform.resume.infrastructure.repository;

import com.shplatform.resume.infrastructure.entity.SchoolEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SchoolRepository extends JpaRepository<SchoolEntity, Long> {

    List<SchoolEntity> findTop20ByNameContainingOrderByNameAsc(String name);

    List<SchoolEntity> findTop20ByNameContainingAndSchoolTypeOrderByNameAsc(
            String name, String schoolType);
}
