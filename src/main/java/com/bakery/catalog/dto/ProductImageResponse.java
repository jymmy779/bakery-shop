package com.bakery.catalog.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProductImageResponse {

  private Long id;
  private String imagePath;
  private int displayOrder;
}
