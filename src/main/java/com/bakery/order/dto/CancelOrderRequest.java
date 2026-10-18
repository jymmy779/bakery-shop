package com.bakery.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CancelOrderRequest {

  @NotBlank(message = "Vui lòng nhập lý do hủy đơn")
  @Size(max = 500)
  private String reason;
}
