package com.shplatform.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.shplatform.auth.infrastructure.MenuEntity;
import com.shplatform.auth.infrastructure.MenuRepository;
import com.shplatform.shared.exception.BusinessException;
import com.shplatform.shared.exception.ErrorCode;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MenuServiceTest {

    private MenuRepository repository;
    private MenuService service;

    @BeforeEach
    void setUp() {
        repository = mock(MenuRepository.class);
        service = new MenuService(repository);
    }

    private MenuEntity entity(String itemId, String roles) {
        MenuEntity e = new MenuEntity();
        set(e, "app", "scraper");
        set(e, "itemId", itemId);
        set(e, "label", "라벨-" + itemId);
        set(e, "href", "/");
        set(e, "icon", "Search");
        set(e, "external", false);
        set(e, "primary", true);
        set(e, "section", "검색");
        set(e, "itemOrder", 10);
        set(e, "roles", roles);
        set(e, "visible", true);
        return e;
    }

    private static void set(Object target, String field, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Test
    void getMenu_매핑_정상() {
        given(repository.findByAppOrderByItemOrderAsc("scraper"))
                .willReturn(List.of(entity("scraper.search", null), entity("scraper.admin", "ADMIN")));

        var result = service.getMenu("scraper");

        assertThat(result.app()).isEqualTo("scraper");
        assertThat(result.items()).hasSize(2);
        var first = result.items().get(0);
        assertThat(first.id()).isEqualTo("scraper.search");
        assertThat(first.label()).isEqualTo("라벨-scraper.search");
        assertThat(first.roles()).isNull();
        assertThat(first.primary()).isTrue();
        assertThat(first.external()).isFalse();
        assertThat(first.visible()).isTrue();
        assertThat(result.items().get(1).roles()).containsExactly("ADMIN");
    }

    @Test
    void getMenu_메뉴없으면_빈목록() {
        given(repository.findByAppOrderByItemOrderAsc("auth")).willReturn(List.of());

        var result = service.getMenu("auth");

        assertThat(result.app()).isEqualTo("auth");
        assertThat(result.items()).isEmpty();
    }

    @Test
    void getMenu_알수없는앱_예외() {
        assertThatThrownBy(() -> service.getMenu("unknown"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);

        assertThatThrownBy(() -> service.getMenu(null))
                .isInstanceOf(BusinessException.class);
    }
}
