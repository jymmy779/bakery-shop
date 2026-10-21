package com.bakery.catalog.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bakery.catalog.dto.CategoryRequest;
import com.bakery.catalog.entity.Category;
import com.bakery.catalog.repository.CategoryRepository;
import com.bakery.catalog.repository.ProductRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

  @Mock CategoryRepository categories;
  @Mock ProductRepository products;

  private CategoryServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new CategoryServiceImpl(categories, products);
  }

  @Test
  void createsCategory() {
    CategoryRequest request = request("  Bánh kem  ", "Mô tả");
    Category saved = new Category();
    saved.setId(1L);
    saved.setName("Bánh kem");
    saved.setDescription("Mô tả");
    saved.setActive(true);
    when(categories.findByNameIgnoreCase("Bánh kem")).thenReturn(Optional.empty());
    when(categories.save(org.mockito.ArgumentMatchers.any(Category.class))).thenReturn(saved);

    var result = service.create(request);

    assertEquals(1L, result.getId());
    assertEquals("Bánh kem", result.getName());
    verify(categories).save(org.mockito.ArgumentMatchers.any(Category.class));
  }

  @Test
  void updatesCategory() {
    Category category = category(1L, "Cũ");
    when(categories.findById(1L)).thenReturn(Optional.of(category));
    when(categories.findByNameIgnoreCase("Mới")).thenReturn(Optional.empty());

    var result = service.update(1L, request("Mới", "Mô tả mới"));

    assertEquals("Mới", result.getName());
    assertEquals("Mô tả mới", result.getDescription());
  }

  @Test
  void togglesCategoryStatusInBothDirections() {
    Category category = category(1L, "Bánh");
    when(categories.findById(1L)).thenReturn(Optional.of(category));

    service.toggleStatus(1L);
    assertEquals(false, category.isActive());

    service.toggleStatus(1L);
    assertEquals(true, category.isActive());
  }

  @Test
  void returnsInactiveCategoriesForCms() {
    Category active = category(1L, "Đang bán");
    Category inactive = category(2L, "Ngừng hoạt động");
    inactive.setActive(false);
    when(categories.findAll()).thenReturn(java.util.List.of(active, inactive));

    assertEquals(2, service.findAll().size());
  }

  @Test
  void rejectsDeletingCategoryThatContainsProducts() {
    when(products.countByCategoryId(1L)).thenReturn(2L);

    assertThrows(IllegalStateException.class, () -> service.delete(1L));
  }

  @Test
  void deletesEmptyCategory() {
    Category category = category(1L, "Bánh");
    when(products.countByCategoryId(1L)).thenReturn(0L);
    when(categories.findById(1L)).thenReturn(Optional.of(category));

    service.delete(1L);

    verify(categories).delete(category);
  }

  private CategoryRequest request(String name, String description) {
    CategoryRequest request = new CategoryRequest();
    request.setName(name);
    request.setDescription(description);
    return request;
  }

  private Category category(Long id, String name) {
    Category category = new Category();
    category.setId(id);
    category.setName(name);
    category.setActive(true);
    return category;
  }
}
