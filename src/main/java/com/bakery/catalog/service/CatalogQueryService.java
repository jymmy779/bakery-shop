package com.bakery.catalog.service;

import com.bakery.catalog.dto.ProductSummaryResponse;
import com.bakery.catalog.dto.ProductDetailResponse;
import com.bakery.catalog.dto.ProductImageResponse;
import com.bakery.catalog.dto.CategoryResponse;
import com.bakery.common.exception.ResourceNotFoundException;
import com.bakery.catalog.entity.Product;
import com.bakery.catalog.repository.ProductImageRepository;
import com.bakery.catalog.repository.ProductRepository;
import java.util.HashMap;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CatalogQueryService {
  private final ProductRepository products;
  private final ProductImageRepository images;

  @Transactional(readOnly = true)
  public ProductDetailResponse getById(Long productId) {
    Product product = products.findById(productId)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bánh"));
    var result = new ProductDetailResponse();
    result.setId(product.getId());
    result.setName(product.getName());
    result.setDescription(product.getDescription());
    result.setPrice(product.getPrice());
    result.setStockQuantity(product.getStockQuantity());
    result.setPreparationDays(product.getPreparationDays());
    result.setStatus(product.getStatus());
    var category = new CategoryResponse();
    category.setId(product.getCategory().getId());
    category.setName(product.getCategory().getName());
    category.setDescription(product.getCategory().getDescription());
    category.setActive(product.getCategory().isActive());
    result.setCategory(category);
    result.setImages(images.findByProductIdInOrderByDisplayOrderAscIdAsc(List.of(productId))
        .stream().map(image -> {
          var response = new ProductImageResponse();
          response.setId(image.getId());
          response.setImagePath(image.getImagePath());
          response.setDisplayOrder(image.getDisplayOrder());
          return response;
        }).toList());
    return result;
  }

  @Transactional(readOnly = true)
  public List<ProductSummaryResponse> list() {
    return list(null);
  }

  @Transactional(readOnly = true)
  public List<ProductSummaryResponse> list(String keyword) {
    Sort sort = Sort.by("name", "id");
    List<Product> productList = keyword == null || keyword.isBlank()
        ? products.findAll(sort)
        : products.searchByNameOrDescription("%" + keyword.strip()
            .replace('đ', 'd').replace('Đ', 'D')
            .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%");
    var primaryImages = new HashMap<Long, String>();
    if (!productList.isEmpty()) {
      images.findByProductIdInOrderByDisplayOrderAscIdAsc(productList.stream().map(Product::getId).toList())
          .forEach(image -> primaryImages.putIfAbsent(image.getProduct().getId(), image.getImagePath()));
    }
    return productList.stream().map(product -> {
      var result = new ProductSummaryResponse();
      result.setId(product.getId());
      result.setName(product.getName());
      result.setPrice(product.getPrice());
      result.setStockQuantity(product.getStockQuantity());
      result.setPreparationDays(product.getPreparationDays());
      result.setStatus(product.getStatus());
      result.setCategoryId(product.getCategory().getId());
      result.setCategoryName(product.getCategory().getName());
      result.setPrimaryImagePath(primaryImages.get(product.getId()));
      return result;
    }).toList();
  }
}
