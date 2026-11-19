package com.bakery.order.service.impl;

import com.bakery.order.dto.OrderMetricsResponse;
import com.bakery.order.entity.OrderStatus;
import com.bakery.order.repository.OrderRepository;
import com.bakery.order.service.OrderMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderMetricsServiceImpl implements OrderMetricsService {

  private final OrderRepository orderRepository;

  @Override
  @Transactional(readOnly = true)
  public OrderMetricsResponse getOrderMetrics() {
    long pending = orderRepository.countByStatus(OrderStatus.PENDING);
    long confirmed = orderRepository.countByStatus(OrderStatus.CONFIRMED);
    long delivering = orderRepository.countByStatus(OrderStatus.DELIVERING);
    return new OrderMetricsResponse(pending, confirmed, delivering);
  }
}
