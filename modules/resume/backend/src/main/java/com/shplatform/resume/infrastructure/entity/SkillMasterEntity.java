package com.shplatform.resume.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기술 스택 마스터 데이터 — DB 코드화, 프론트 하드코딩 금지.
 * 유사어(별칭)를 포함해 이력서 기술 스택 입력 시 자동완성/유사검색에 사용된다.
 */
@Entity
@Table(name = "resume_skill_master")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SkillMasterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "aliases", length = 500)
    private String aliases;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "active", nullable = false)
    private Boolean active = true;
}
