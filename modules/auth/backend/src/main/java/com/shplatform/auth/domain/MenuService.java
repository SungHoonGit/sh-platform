package com.shplatform.auth.domain;

import com.shplatform.auth.api.dto.MenuResponse;
import com.shplatform.auth.infrastructure.MenuEntity;
import com.shplatform.auth.infrastructure.MenuRepository;
import com.shplatform.shared.exception.BusinessException;
import com.shplatform.shared.exception.ErrorCode;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * (조회형) 앱별 메뉴를 조회한다 (DB 단일 소스 — shell AppMenu와 동일 스키마).
 */
@Service
public class MenuService {

    private static final Set<String> APPS = Set.of("scraper", "resume", "platform", "auth");

    private final MenuRepository repository;

    public MenuService(MenuRepository repository) {
        this.repository = repository;
    }

    /**
     * (조회형) 앱별 메뉴를 표시 순서대로 조회한다.
     *
     * @param app 앱 코드 (scraper / resume / platform / auth)
     * @return 앱 메뉴 (해당 앱 메뉴가 없으면 빈 목록)
     * @throws BusinessException INVALID_INPUT — 알 수 없는 app
     */
    public MenuResponse getMenu(String app) {
        if (app == null || !APPS.contains(app)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        List<MenuResponse.Item> items = repository.findByAppOrderByItemOrderAsc(app).stream()
                .map(this::toItem)
                .toList();
        return new MenuResponse(app, items);
    }

    private MenuResponse.Item toItem(MenuEntity e) {
        return new MenuResponse.Item(
                e.getItemId(), e.getLabel(), e.getHref(), e.getIcon(),
                e.isExternal(), e.isPrimary(), e.getSection(), e.getItemOrder(),
                parseRoles(e.getRoles()), e.isVisible());
    }

    /** roles 컬럼(CSV 또는 NULL) → 목록 (NULL은 필드 생략용으로 null 반환) */
    private List<String> parseRoles(String roles) {
        if (roles == null || roles.isBlank()) return null;
        return List.of(roles.split(","));
    }
}
