package com.bakery.catalog.dto;

import com.bakery.catalog.entity.ProductStatus;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProductSummaryResponse {

  private Long id;
  private String name;
  private String description;
  private BigDecimal price;
  private int stockQuantity;
  private int preparationDays;
  private ProductStatus status;
  private Long categoryId;
  private String categoryName;
  private String primaryImagePath;
}
