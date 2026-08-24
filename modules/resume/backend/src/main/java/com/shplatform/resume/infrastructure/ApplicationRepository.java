package com.shplatform.resume.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<ApplicationEntity, Long> {

    List<ApplicationEntity> findByUserIdOrderByAppliedAtDescIdDesc(Long userId);

    List<ApplicationEntity> findByUserIdAndStatusOrderByAppliedAtDescIdDesc(Long userId, String status);
}
