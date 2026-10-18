package com.bakery.cart.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CartItemResponse {

  private Long productId;
  private String productName;
  private String imagePath;
  private BigDecimal unitPrice;
  private int quantity;
  private BigDecimal lineTotal;
  private int availableStock;
  private int preparationDays;
}
