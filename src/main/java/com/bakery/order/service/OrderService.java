package com.bakery.order.service;

import com.bakery.order.dto.CancelOrderRequest;
import com.bakery.order.dto.OrderDetailResponse;
import com.bakery.order.dto.OrderSummaryResponse;
import com.bakery.order.dto.OrderStatusUpdateRequest;
import com.bakery.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

  OrderDetailResponse getCustomerOrder(Long accountId, Long orderId);

  Page<OrderSummaryResponse> findCustomerOrders(
      Long accountId,
      OrderStatus status,
      Pageable pageable);

  OrderDetailResponse cancel(Long accountId, Long orderId, CancelOrderRequest request);

  Page<OrderSummaryResponse> findAll(OrderStatus status, Pageable pageable);

  OrderDetailResponse updateStatus(
      Long orderId,
      OrderStatusUpdateRequest request,
      Long changedByAccountId);
}
