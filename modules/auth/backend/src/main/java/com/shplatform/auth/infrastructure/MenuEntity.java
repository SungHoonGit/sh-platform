package com.shplatform.auth.infrastructure;

import jakarta.persistence.*;

@Entity
@Table(name = "menus", uniqueConstraints = {
        @UniqueConstraint(name = "uk_menus_app_item", columnNames = {"app", "item_id"})
}, indexes = {
        @Index(name = "idx_menus_app_order", columnList = "app, item_order")
})
public class MenuEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "app", nullable = false, length = 32)
    private String app;

    @Column(name = "item_id", nullable = false, length = 64)
    private String itemId;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(length = 255)
    private String href;

    @Column(length = 64)
    private String icon;

    @Column(name = "is_external", nullable = false)
    private boolean external;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @Column(length = 64)
    private String section;

    @Column(name = "item_order", nullable = false)
    private int itemOrder;

    /** NULL/ALL = 전체, ADMIN = 관리자만 (쉼표 구분 대비) */
    @Column(length = 32)
    private String roles;

    @Column(nullable = false)
    private boolean visible = true;

    public String getApp() { return app; }
    public String getItemId() { return itemId; }
    public String getLabel() { return label; }
    public String getHref() { return href; }
    public String getIcon() { return icon; }
    public boolean isExternal() { return external; }
    public boolean isPrimary() { return primary; }
    public String getSection() { return section; }
    public int getItemOrder() { return itemOrder; }
    public String getRoles() { return roles; }
    public boolean isVisible() { return visible; }
}
