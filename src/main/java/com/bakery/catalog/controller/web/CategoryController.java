package com.bakery.catalog.controller.web;

import com.bakery.catalog.dto.CategoryResponse;
import com.bakery.catalog.service.CategoryAdminService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
  private final CategoryAdminService categories;

  @GetMapping
  public List<CategoryResponse> list() {
    return categories.findAllActive();
  }
}
