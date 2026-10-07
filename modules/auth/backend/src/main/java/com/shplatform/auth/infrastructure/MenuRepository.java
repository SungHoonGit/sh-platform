package com.shplatform.auth.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository extends JpaRepository<MenuEntity, Long> {

    /**
     * (조회형) 앱별 메뉴를 표시 순서대로 조회한다.
     *
     * @param app 앱 코드 (scraper / resume / platform / auth)
     * @return 해당 앱의 메뉴 목록 (없으면 빈 목록)
     */
    List<MenuEntity> findByAppOrderByItemOrderAsc(String app);
}
