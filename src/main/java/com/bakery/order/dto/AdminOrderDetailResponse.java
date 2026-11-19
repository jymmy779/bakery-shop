package com.bakery.order.dto;

public record AdminOrderDetailResponse(OrderDetailResponse order, boolean canConfirmPayment) {}
