package com.bakery.catalog.service;

import com.bakery.catalog.dto.ProductCreateRequest;
import com.bakery.catalog.dto.ProductDetailResponse;
import com.bakery.catalog.dto.ProductSummaryResponse;
import com.bakery.catalog.dto.ProductUpdateRequest;
import com.bakery.catalog.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CatalogAdminProductService {

  Page<ProductSummaryResponse> getAllProducts(
      String keyword, Long categoryId, ProductStatus status, Pageable pageable);

  ProductDetailResponse getById(Long productId);

  ProductDetailResponse createProduct(ProductCreateRequest request);

  ProductDetailResponse updateProduct(Long productId, ProductUpdateRequest request);

  void stopSellingProduct(Long productId);

  void deleteProduct(Long productId);
}
