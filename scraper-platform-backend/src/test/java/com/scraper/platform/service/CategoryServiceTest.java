package com.scraper.platform.service;

import com.scraper.platform.model.Category;
import com.scraper.platform.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService 테스트")
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        testCategory = Category.builder()
                .id(1L)
                .name("Java")
                .slug("java")
                .description("Java 관련 문서")
                .build();
    }

    @Nested
    @DisplayName("getAllCategories 메서드")
    class GetAllCategories {

        @Test
        @DisplayName("전체 카테고리를 조회한다")
        void getAllCategories_shouldReturnAll() {
            given(categoryRepository.findAll()).willReturn(java.util.List.of(testCategory));

            var result = categoryService.getAllCategories();

            assertEquals(1, result.size());
            assertEquals("Java", result.get(0).getName());
        }
    }

    @Nested
    @DisplayName("getCategoryBySlug 메서드")
    class GetCategoryBySlug {

        @Test
        @DisplayName("슬러그로 카테고리를 조회한다")
        void getCategoryBySlug_shouldReturnCategory_whenExists() {
            given(categoryRepository.findBySlug("java")).willReturn(Optional.of(testCategory));

            var result = categoryService.getCategoryBySlug("java");

            assertNotNull(result);
            assertEquals("Java", result.getName());
        }

        @Test
        @DisplayName("존재하지 않는 슬러그 조회 시 예외 발생")
        void getCategoryBySlug_shouldThrow_whenNotExists() {
            given(categoryRepository.findBySlug("nonexistent")).willReturn(Optional.empty());

            assertThrows(RuntimeException.class,
                    () -> categoryService.getCategoryBySlug("nonexistent"));
        }
    }

    @Nested
    @DisplayName("createCategory 메서드")
    class CreateCategory {

        @Test
        @DisplayName("정상적으로 카테고리를 생성한다")
        void createCategory_shouldCreate_whenValidInput() {
            given(categoryRepository.existsBySlug("new")).willReturn(false);
            when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

            var result = categoryService.createCategory("Java", "new", "설명");

            assertNotNull(result);
            verify(categoryRepository).save(any(Category.class));
        }

        @Test
        @DisplayName("중복 슬러그로 생성 시 예외 발생")
        void createCategory_shouldThrow_whenDuplicateSlug() {
            given(categoryRepository.existsBySlug("java")).willReturn(true);

            assertThrows(RuntimeException.class,
                    () -> categoryService.createCategory("Java", "java", "설명"));
        }
    }
}
