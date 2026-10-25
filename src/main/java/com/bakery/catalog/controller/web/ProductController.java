package com.bakery.catalog.controller.web;

import com.bakery.catalog.service.ProductQueryService;
import com.bakery.catalog.dto.ProductDetailResponse;
import com.bakery.catalog.dto.ProductListResponse;
import com.bakery.catalog.dto.ProductFilters;
import java.math.BigDecimal;
import java.util.Locale;
import com.bakery.common.exception.ResourceNotFoundException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestController
@RequestMapping(value = "/api/products", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ProductController {
  private final ProductQueryService productQueryService;

  @GetMapping
  public ProductListResponse list(@RequestParam(defaultValue = "") String keyword,
      @RequestParam(required = false) Long categoryId,
      @RequestParam(required = false) BigDecimal minPrice,
      @RequestParam(required = false) BigDecimal maxPrice,
      @RequestParam(defaultValue = "name") String sort,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "12") int size) {
    keyword = keyword.strip();
    sort = sort.strip().toLowerCase(Locale.ROOT);
    var result = productQueryService.filter(keyword, categoryId, minPrice, maxPrice, sort, page, size);
    String message = result.isEmpty()
        ? "Không tìm thấy sản phẩm phù hợp"
        : "Tìm thấy " + result.getTotalElements() + " sản phẩm";
    var filters = new ProductFilters(keyword, categoryId, minPrice, maxPrice, sort);
    return new ProductListResponse(message, result.getContent(), page, size,
        result.getTotalElements(), result.getTotalPages(), filters,
        result.hasNext() ? pageLink(filters, page + 1, size) : null,
        result.hasPrevious() ? pageLink(filters, page - 1, size) : null);
  }

  private String pageLink(ProductFilters filters, int page, int size) {
    var uri = UriComponentsBuilder.fromPath("/api/products")
        .queryParam("keyword", filters.keyword()).queryParam("sort", filters.sort())
        .queryParam("page", page).queryParam("size", size);
    if (filters.categoryId() != null) uri.queryParam("categoryId", filters.categoryId());
    if (filters.minPrice() != null) uri.queryParam("minPrice", filters.minPrice());
    if (filters.maxPrice() != null) uri.queryParam("maxPrice", filters.maxPrice());
    return uri.build().encode().toUriString();
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> invalidFilter(IllegalArgumentException exception) {
    return ResponseEntity.badRequest().body(Map.of("status", 400, "message", exception.getMessage()));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<Map<String, Object>> invalidParameter(MethodArgumentTypeMismatchException exception) {
    return ResponseEntity.badRequest().body(Map.of("status", 400,
        "message", "Tham số " + exception.getName() + " không hợp lệ"));
  }

  @GetMapping("/{productId}")
  public ProductDetailResponse detail(@PathVariable Long productId) {
    return productQueryService.getById(productId);
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<Map<String, Object>> notFound(ResourceNotFoundException exception) {
    return ResponseEntity.status(404).body(Map.of("status", 404, "message", exception.getMessage()));
  }
}
