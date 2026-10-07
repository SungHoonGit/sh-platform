package com.shplatform.auth.api;

import com.shplatform.auth.api.dto.MenuResponse;
import com.shplatform.auth.domain.MenuService;
import com.shplatform.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Menu", description = "앱 메뉴 조회 API - shell AppMenu와 동일 스키마 (DB 단일 소스)")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/menus")
    @Operation(summary = "앱 메뉴 조회", description = "app 파라미터로 앱별 메뉴 목록을 반환한다. 미로그인 접근 가능(퍼블릭).")
    public ResponseEntity<ApiResponse<MenuResponse>> getMenus(@RequestParam String app) {
        return ResponseEntity.ok(ApiResponse.success(menuService.getMenu(app)));
    }
}
