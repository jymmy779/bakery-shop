package com.bakery.order.service;

import com.bakery.order.dto.OrderStatusUpdateRequest;

public interface AdminOrderStatusService {

  void updateOrderStatus(Long staffId, Long orderId, OrderStatusUpdateRequest request);
}
