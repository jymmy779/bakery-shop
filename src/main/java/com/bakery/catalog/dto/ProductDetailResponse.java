package com.bakery.catalog.dto;

import com.bakery.catalog.entity.ProductStatus;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProductDetailResponse {

  private Long id;
  private String name;
  private String description;
  private BigDecimal price;
  private int stockQuantity;
  private int preparationDays;
  private ProductStatus status;
  private CategoryResponse category;
  private List<ProductImageResponse> images = new ArrayList<>();
}
