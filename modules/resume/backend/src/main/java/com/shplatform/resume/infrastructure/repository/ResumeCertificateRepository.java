package com.shplatform.resume.infrastructure.repository;

import com.shplatform.resume.infrastructure.entity.ResumeCertificateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResumeCertificateRepository extends JpaRepository<ResumeCertificateEntity, Long> {

    List<ResumeCertificateEntity> findByUserIdOrderByDisplayOrderAscIdAsc(Long userId);
}
