package com.bakery.catalog.service;

import com.bakery.catalog.dto.ProductDetailResponse;
import com.bakery.catalog.dto.ProductSummaryResponse;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;

public interface ProductQueryService {

  Page<ProductSummaryResponse> filter(String keyword, Long categoryId,
      BigDecimal minPrice, BigDecimal maxPrice, String sort, int page, int size);

  ProductDetailResponse getById(Long productId);

  List<ProductSummaryResponse> list();

  List<ProductSummaryResponse> list(String keyword);
}
