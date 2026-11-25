package com.bakery.order.service.impl;

import com.bakery.common.exception.ResourceNotFoundException;
import com.bakery.order.dto.OrderDetailResponse;
import com.bakery.order.entity.Order;
import com.bakery.order.repository.OrderDetailRepository;
import com.bakery.order.repository.OrderRepository;
import com.bakery.order.repository.PaymentRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheckoutResultServiceImpl {

  private final OrderRepository orders;
  private final OrderDetailRepository details;
  private final PaymentRepository payments;
  private final CheckoutOrderMapper mapper;

  public OrderDetailResponse getForCustomer(Long accountId, Long orderId) {
    return map(orders.findByIdAndAccount_Id(orderId, accountId)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng của bạn")));
  }

  public Optional<OrderDetailResponse> findSubmitted(Long accountId, String token) {
    return orders.findByOrderCodeAndAccount_Id(CheckoutQuoteStore.orderCode(token), accountId)
        .map(this::map);
  }

  private OrderDetailResponse map(Order order) {
    return mapper.map(order, details.findByOrder_IdOrderByIdAsc(order.getId()),
        payments.findByOrder_Id(order.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin thanh toán")));
  }
}
