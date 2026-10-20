package com.bakery.catalog.service;

import com.bakery.catalog.dto.CategoryRequest;
import com.bakery.catalog.dto.CategoryResponse;
import java.util.List;

public interface CategoryService {

  List<CategoryResponse> getAllCategories();

  CategoryResponse createCategory(CategoryRequest request);

  CategoryResponse updateCategory(Long categoryId, CategoryRequest request);

  void deleteCategory(Long categoryId);

  default CategoryResponse create(CategoryRequest request) {
    return createCategory(request);
  }

  default CategoryResponse update(Long categoryId, CategoryRequest request) {
    return updateCategory(categoryId, request);
  }

  CategoryResponse getById(Long categoryId);

  List<CategoryResponse> findAllActive();

  default void delete(Long categoryId) {
    deleteCategory(categoryId);
  }
}
