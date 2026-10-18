package com.bakery.cart.model;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CartItem implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private Long productId;
  private String productName;
  private String imagePath;
  private BigDecimal unitPrice;
  private int quantity;
  private int availableStock;
  private int preparationDays;
}
