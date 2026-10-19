package com.bakery.catalog.controller.web;

import com.bakery.catalog.service.CatalogQueryService;
import com.bakery.catalog.dto.ProductDetailResponse;
import com.bakery.catalog.dto.ProductSummaryResponse;
import com.bakery.common.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/products", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ProductController {
  private final CatalogQueryService catalog;

  @GetMapping
  public List<ProductSummaryResponse> list() {
    return catalog.list();
  }

  @GetMapping("/{productId}")
  public ProductDetailResponse detail(@PathVariable Long productId) {
    return catalog.getById(productId);
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<Map<String, Object>> notFound(ResourceNotFoundException exception) {
    return ResponseEntity.status(404).body(Map.of("status", 404, "message", exception.getMessage()));
  }
}
