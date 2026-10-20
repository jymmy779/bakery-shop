package com.bakery.catalog.service;

import com.bakery.catalog.dto.ProductCreateRequest;
import com.bakery.catalog.dto.ProductDetailResponse;
import com.bakery.catalog.dto.ProductSummaryResponse;
import com.bakery.catalog.dto.ProductUpdateRequest;
import com.bakery.catalog.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {

  ProductDetailResponse create(ProductCreateRequest request);

  ProductDetailResponse update(Long productId, ProductUpdateRequest request);

  ProductDetailResponse getById(Long productId);

  Page<ProductSummaryResponse> search(
      String keyword,
      Long categoryId,
      ProductStatus status,
      Pageable pageable);

  void deleteOrStopSelling(Long productId);
}
