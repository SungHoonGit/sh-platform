package com.scraper.platform.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "site_parameter_definition")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SiteParameterDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_definition_id", nullable = false)
    private SiteDefinition siteDefinition;

    @Column(name = "param_key", nullable = false, length = 50)
    private String paramKey;

    @Column(name = "param_name", nullable = false, length = 100)
    private String paramName;

    @Enumerated(EnumType.STRING)
    @Column(name = "param_type", length = 20)
    @Builder.Default
    private ParamType paramType = ParamType.text;

    @Column(name = "is_required")
    @Builder.Default
    private Boolean isRequired = false;

    @Column(columnDefinition = "TEXT")
    private String options;

    @Column(name = "default_value", length = 200)
    private String defaultValue;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ParamType {
        text, select, hidden, tags
    }
}
