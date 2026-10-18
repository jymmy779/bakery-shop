package com.bakery.cart.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AddToCartRequest {

  @NotNull(message = "Thiếu mã sản phẩm")
  private Long productId;

  @NotNull(message = "Vui lòng nhập số lượng")
  @Positive(message = "Số lượng phải lớn hơn 0")
  private Integer quantity;
}
