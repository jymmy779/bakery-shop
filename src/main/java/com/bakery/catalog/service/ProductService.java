package com.bakery.catalog.service;

import com.bakery.catalog.dto.ProductCreateRequest;
import com.bakery.catalog.dto.ProductDetailResponse;
import com.bakery.catalog.dto.ProductSummaryResponse;
import com.bakery.catalog.dto.ProductUpdateRequest;
import com.bakery.catalog.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {

  Page<ProductSummaryResponse> getAllProducts(String keyword, Long categoryId,
      ProductStatus status, Pageable pageable);

  ProductDetailResponse createProduct(ProductCreateRequest request);

  ProductDetailResponse updateProduct(Long productId, ProductUpdateRequest request);

  void stopSellingProduct(Long productId);

  void deleteProduct(Long productId);

  default ProductDetailResponse create(ProductCreateRequest request) {
    return createProduct(request);
  }

  default ProductDetailResponse update(Long productId, ProductUpdateRequest request) {
    return updateProduct(productId, request);
  }

  ProductDetailResponse getById(Long productId);

  default Page<ProductSummaryResponse> search(
      String keyword,
      Long categoryId,
      ProductStatus status,
      Pageable pageable) {
    return getAllProducts(keyword, categoryId, status, pageable);
  }

  default void deleteOrStopSelling(Long productId) {
    deleteProduct(productId);
  }
}
