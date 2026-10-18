package com.bakery.cart.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CartResponse {

  private List<CartItemResponse> items = new ArrayList<>();
  private int totalQuantity;
  private BigDecimal totalAmount;
  private int maximumPreparationDays;
}
