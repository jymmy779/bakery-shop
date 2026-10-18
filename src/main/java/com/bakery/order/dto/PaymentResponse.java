package com.bakery.order.dto;

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
public class PaymentResponse {

  private PaymentMethod method;
  private PaymentStatus status;
  private BigDecimal amount;
  private String bankReference;
  private LocalDateTime paidAt;
  private String confirmedByName;
}
