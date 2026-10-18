package com.bakery.order.dto;

import com.bakery.order.entity.OrderStatus;
import com.bakery.order.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrderSummaryResponse {

  private Long id;
  private String orderCode;
  private String customerName;
  private BigDecimal totalAmount;
  private OrderStatus status;
  private PaymentStatus paymentStatus;
  private LocalDate deliveryDate;
  private LocalDateTime createdAt;
}
