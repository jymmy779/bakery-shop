package com.bakery.order.dto;

import com.bakery.order.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrderStatusUpdateRequest {

  @NotNull(message = "Vui lòng chọn trạng thái đơn hàng")
  private OrderStatus status;

  @Size(max = 500)
  private String note;
}
