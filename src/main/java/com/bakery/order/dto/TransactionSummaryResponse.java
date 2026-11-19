package com.bakery.order.dto;

import com.bakery.order.entity.OrderStatus;
import com.bakery.order.entity.PaymentMethod;
import com.bakery.order.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TransactionSummaryResponse {

  private Long paymentId;
  private Long orderId;
  private String orderCode;
  private String customerName;
  private PaymentMethod method;
  private PaymentStatus paymentStatus;
  private OrderStatus orderStatus;
  private BigDecimal amount;
  private String bankReference;
  private LocalDateTime paidAt;
  private LocalDateTime createdAt;
  private String confirmedByName;
}
