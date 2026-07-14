package com.scraper.platform.service;

import com.scraper.platform.model.Category;
import com.scraper.platform.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category getCategoryBySlug(String slug) {
        return categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Category not found: " + slug));
    }

    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found: " + id));
    }

    @Transactional
    public Category createCategory(String name, String slug, String description) {
        if (categoryRepository.existsBySlug(slug)) {
            throw new RuntimeException("Duplicate slug: " + slug);
        }
        Category category = Category.builder()
                .name(name)
                .slug(slug)
                .description(description)
                .build();
        return categoryRepository.save(category);
    }
}
