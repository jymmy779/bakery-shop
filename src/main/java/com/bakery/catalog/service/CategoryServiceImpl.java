package com.bakery.catalog.service;

import com.bakery.catalog.dto.CategoryRequest;
import com.bakery.catalog.dto.CategoryResponse;
import com.bakery.catalog.entity.Category;
import com.bakery.catalog.repository.CategoryRepository;
import com.bakery.common.exception.ResourceNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
  private final CategoryRepository categories;

  @Override
  @Transactional
  public CategoryResponse create(CategoryRequest request) {
    String name = request.getName().trim();
    if (categories.findByNameIgnoreCase(name).isPresent()) {
      throw new IllegalArgumentException("Tên danh mục đã tồn tại!");
    }
    Category category = new Category();
    category.setName(name);
    category.setDescription(request.getDescription());
    category.setActive(true);
    return toResponse(categories.save(category));
  }

  @Override
  @Transactional
  public CategoryResponse update(Long categoryId, CategoryRequest request) {
    Category category = find(categoryId);
    String name = request.getName().trim();
    categories.findByNameIgnoreCase(name).filter(existing -> !existing.getId().equals(categoryId))
        .ifPresent(existing -> { throw new IllegalArgumentException("Tên danh mục đã tồn tại!"); });
    category.setName(name);
    category.setDescription(request.getDescription());
    return toResponse(category);
  }

  @Override
  @Transactional(readOnly = true)
  public CategoryResponse getById(Long categoryId) {
    return toResponse(find(categoryId));
  }

  @Override
  @Transactional(readOnly = true)
  public List<CategoryResponse> findAllActive() {
    return categories.findAll().stream().filter(Category::isActive).map(this::toResponse).toList();
  }

  @Override
  @Transactional
  public void delete(Long categoryId) {
    categories.delete(find(categoryId));
  }

  private Category find(Long id) {
    return categories.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));
  }

  private CategoryResponse toResponse(Category category) {
    CategoryResponse response = new CategoryResponse();
    response.setId(category.getId());
    response.setName(category.getName());
    response.setDescription(category.getDescription());
    response.setActive(category.isActive());
    return response;
  }
}