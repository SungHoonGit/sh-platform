package com.shplatform.resume.infrastructure.repository;

import com.shplatform.resume.infrastructure.entity.SkillMasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SkillMasterRepository extends JpaRepository<SkillMasterEntity, Long> {

    /**
     * 활성 기술 스택을 입력어에 대해 유사검색한다.
     * 정식 이름 부분일치 + 별칭 포함을 함께 찾고, 이름 일치가 앞에 오도록 정렬한다.
     *
     * @param q  검색어 (대소문자 무시)
     * @param limit 최대 결과 수
     */
    @Query(value = """
            SELECT * FROM resume_skill_master
            WHERE active = 1
              AND (LOWER(name) LIKE CONCAT('%', LOWER(:q), '%')
                   OR LOWER(aliases) LIKE CONCAT('%', LOWER(:q), '%'))
            ORDER BY
                (CASE WHEN LOWER(name) = LOWER(:q) THEN 0
                      WHEN LOWER(name) LIKE CONCAT('%', LOWER(:q), '%') THEN 1
                      ELSE 2 END),
                display_order, name
            LIMIT :limit
            """, nativeQuery = true)
    List<SkillMasterEntity> search(@Param("q") String q, @Param("limit") int limit);

    /**
     * 활성 기술 스택을 표시 순서로 조회한다. (매칭 정규화용 전체 로드)
     */
    List<SkillMasterEntity> findByActiveTrueOrderByDisplayOrderAscNameAsc();
}
