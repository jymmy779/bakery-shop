package com.bakery.catalog.controller.web;

import com.bakery.catalog.dto.CategoryResponse;
import com.bakery.catalog.service.CategoryAdminService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/categories", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class CategoryController {

  private final CategoryAdminService categoryService;

  @GetMapping
  public List<CategoryResponse> listActiveCategories() {
    return categoryService.findAllActive();
  }
}
