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
 * 학교 마스터 데이터 (고등학교/대학교/대학원) — DB 코드화, 프론트 하드코딩 금지.
 */
@Entity
@Table(name = "schools")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SchoolEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "school_type", nullable = false, length = 20)
    private String schoolType;

    public static SchoolEntity of(String name, String schoolType) {
        var e = new SchoolEntity();
        e.name = name;
        e.schoolType = schoolType;
        return e;
    }
}
