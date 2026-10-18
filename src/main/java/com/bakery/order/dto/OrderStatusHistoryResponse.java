package com.bakery.order.dto;

import com.bakery.order.entity.OrderStatus;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrderStatusHistoryResponse {

  private OrderStatus fromStatus;
  private OrderStatus toStatus;
  private String changedByName;
  private String note;
  private LocalDateTime changedAt;
}
