package com.bakery.order.service.impl;

import com.bakery.order.repository.OrderDetailRepository;
import com.bakery.order.service.ProductOrderReferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductOrderReferenceServiceImpl implements ProductOrderReferenceService {

  private final OrderDetailRepository orderDetails;

  @Override
  public boolean hasOrders(Long productId) {
    return orderDetails.existsByProductId(productId);
  }
}
