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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.math.BigDecimal;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CatalogQueryService {
  private final ProductRepository products;
  private final ProductImageRepository images;

  @Transactional(readOnly = true)
  public Page<ProductSummaryResponse> filter(String keyword, Long categoryId,
      BigDecimal minPrice, BigDecimal maxPrice, String sort, int page, int size) {
    if (page < 0 || size < 1 || size > 100) {
      throw new IllegalArgumentException("page phải từ 0, size phải từ 1 đến 100");
    }
    if (categoryId != null && categoryId <= 0) {
      throw new IllegalArgumentException("categoryId phải lớn hơn 0");
    }
    if ((minPrice != null && minPrice.signum() < 0) || (maxPrice != null && maxPrice.signum() < 0)
        || (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0)) {
      throw new IllegalArgumentException("Khoảng giá không hợp lệ: 0 <= minPrice <= maxPrice");
    }
    if (!Set.of("name", "price_asc", "price_desc", "best_selling").contains(sort)) {
      throw new IllegalArgumentException("sort phải là name, price_asc, price_desc hoặc best_selling");
    }
    String pattern = keyword == null || keyword.isBlank() ? null : searchPattern(keyword);
    Page<Product> result = products.filter(pattern, categoryId, minPrice, maxPrice, sort,
        PageRequest.of(page, size));
    return new PageImpl<>(summaries(result.getContent()), result.getPageable(), result.getTotalElements());
  }

  private String searchPattern(String keyword) {
    return "%" + keyword.strip().replace('đ', 'd').replace('Đ', 'D')
        .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
  }

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
        : products.searchByNameOrDescription(searchPattern(keyword));
    return summaries(productList);
  }

  private List<ProductSummaryResponse> summaries(List<Product> productList) {
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
