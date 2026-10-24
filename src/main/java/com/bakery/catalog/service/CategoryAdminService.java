package com.bakery.catalog.service;

import com.bakery.catalog.dto.CategoryRequest;
import com.bakery.catalog.dto.CategoryResponse;
import java.util.List;

public interface CategoryAdminService {

  CategoryResponse create(CategoryRequest request);

  CategoryResponse update(Long categoryId, CategoryRequest request);

  CategoryResponse getById(Long categoryId);

  List<CategoryResponse> findAllActive();

  List<CategoryResponse> findAll();

  void toggleStatus(Long categoryId);

  void delete(Long categoryId);
}
