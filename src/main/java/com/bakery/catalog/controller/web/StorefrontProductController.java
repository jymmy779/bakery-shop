package com.bakery.catalog.controller.web;

import com.bakery.catalog.dto.CategoryResponse;
import com.bakery.catalog.entity.ProductStatus;
import com.bakery.catalog.service.CatalogQueryService;
import com.bakery.catalog.service.CategoryService;
import java.math.BigDecimal;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class StorefrontProductController {
  private final CatalogQueryService catalog;
  private final CategoryService categoryService;

  @GetMapping("/products")
  public String products(@RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "12") int size,
      @RequestParam(defaultValue = "") String keyword,
      @RequestParam(required = false) Long categoryId,
      @RequestParam(required = false) BigDecimal minPrice,
      @RequestParam(required = false) BigDecimal maxPrice,
      @RequestParam(defaultValue = "name") String sort, Model model) {
    String normalizedSort = sort.trim().toLowerCase(Locale.ROOT);
    Page<?> result = catalog.filter(keyword.trim(), categoryId, minPrice, maxPrice,
        normalizedSort, Math.max(page, 0), Math.min(Math.max(size, 1), 100));
    model.addAttribute("products", result.getContent());
    model.addAttribute("productPage", result);
    model.addAttribute("categories", categoryService.findAllActive());
    model.addAttribute("keyword", keyword);
    model.addAttribute("categoryId", categoryId);
    model.addAttribute("minPrice", minPrice);
    model.addAttribute("maxPrice", maxPrice);
    model.addAttribute("sort", normalizedSort);
    model.addAttribute("currentPage", "shop");
    return "products";
  }
}