package com.bakery.order.dto;

import com.bakery.order.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrderDetailResponse {

  private Long id;
  private String orderCode;
  private Long accountId;
  private String receiverName;
  private String receiverPhone;
  private String receiverAddress;
  private String receiverEmail;
  private LocalDate deliveryDate;
  private LocalTime deliveryTime;
  private String note;
  private BigDecimal totalAmount;
  private OrderStatus status;
  private String cancellationReason;
  private LocalDateTime cancelledAt;
  private LocalDateTime createdAt;
  private List<OrderItemResponse> items = new ArrayList<>();
  private PaymentResponse payment;
  private List<OrderStatusHistoryResponse> statusHistory = new ArrayList<>();
}
