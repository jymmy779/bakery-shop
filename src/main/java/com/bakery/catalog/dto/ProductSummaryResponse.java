package com.bakery.catalog.dto;

import com.bakery.catalog.entity.ProductBadge;
import com.bakery.catalog.entity.ProductStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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
  private ProductBadge badge;
  @JsonIgnore
  private String ingredients;
  @JsonIgnore
  private String storageInstructions;
  @JsonIgnore
  private String allergenInfo;
  private BigDecimal price;
  private int stockQuantity;
  private int preparationDays;
  private ProductStatus status;
  private Long categoryId;
  private String categoryName;
  private String primaryImagePath;
  private List<String> imagePaths = new ArrayList<>();
  private List<Long> imageIds = new ArrayList<>();

  public String getImagePathsCsv() {
    return String.join("|", imagePaths);
  }

  public String getImageIdsCsv() {
    return imageIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining("|"));
  }
}
