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
public class TransactionDetailResponse {

  private Long paymentId;
  private Long orderId;
  private String orderCode;
  private BigDecimal amount;
  private PaymentMethod method;
  private PaymentStatus status;
  private String bankReference;
  private LocalDateTime paidAt;
  private LocalDateTime createdAt;
  private String confirmedByName;

  // Thông tin người nhận / đơn hàng liên kết
  private String customerName;
  private String customerPhone;
  private String customerEmail;
  private String receiverAddress;
  private OrderStatus orderStatus;
}
