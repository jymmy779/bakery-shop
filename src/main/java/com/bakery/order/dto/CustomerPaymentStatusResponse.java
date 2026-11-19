package com.bakery.order.dto;

import com.bakery.order.entity.OrderStatus;
import com.bakery.order.entity.PaymentStatus;
import java.time.LocalDateTime;

public record CustomerPaymentStatusResponse(Long orderId, OrderStatus orderStatus,
    PaymentStatus paymentStatus, LocalDateTime paidAt) {}
