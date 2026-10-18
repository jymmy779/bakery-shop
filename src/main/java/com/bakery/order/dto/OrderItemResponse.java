package com.bakery.order.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrderItemResponse {

  private Long productId;
  private String productName;
  private BigDecimal unitPrice;
  private int quantity;
  private BigDecimal lineTotal;
  private String note;
}
