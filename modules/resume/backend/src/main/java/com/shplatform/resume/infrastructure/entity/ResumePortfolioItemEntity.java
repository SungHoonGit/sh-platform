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
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "resume_portfolio_items")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResumePortfolioItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "item_type", nullable = false, length = 20)
    private String itemType;

    @Column(name = "file_path", length = 300)
    private String filePath;

    @Column(name = "link_url", length = 300)
    private String linkUrl;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static ResumePortfolioItemEntity create(Long userId) {
        var entity = new ResumePortfolioItemEntity();
        entity.userId = userId;
        return entity;
    }
}
