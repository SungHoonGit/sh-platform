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
 * 전공 마스터 데이터 — DB 코드화, 프론트 하드코딩 금지.
 */
@Entity
@Table(name = "majors")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MajorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    public static MajorEntity of(String name) {
        var e = new MajorEntity();
        e.name = name;
        return e;
    }
}
