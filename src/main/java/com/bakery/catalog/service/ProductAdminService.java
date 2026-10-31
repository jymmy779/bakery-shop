package com.bakery.catalog.service;

import com.bakery.catalog.dto.ProductCreateRequest;
import com.bakery.catalog.dto.ProductDetailResponse;
import com.bakery.catalog.dto.ProductSummaryResponse;
import com.bakery.catalog.dto.ProductUpdateRequest;
import com.bakery.catalog.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductAdminService {

  ProductDetailResponse create(ProductCreateRequest request);

  ProductDetailResponse update(Long productId, ProductUpdateRequest request);

  ProductDetailResponse getById(Long productId);

  Page<ProductSummaryResponse> search(
      String keyword,
      Long categoryId,
      ProductStatus status,
      String sort,
      Pageable pageable);

  ProductRemovalResult deleteOrStopSelling(Long productId);

  void toggleStatus(Long productId);

  void deleteImage(Long productId, Long imageId);
}
