package com.bakery.order.service.impl;

import com.bakery.order.repository.PaymentRepository;
import com.bakery.order.service.RevenueQueryService;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RevenueQueryServiceImpl implements RevenueQueryService {

  private final PaymentRepository payments;

  @Override
  @Transactional(readOnly = true)
  public BigDecimal deliveredPaidRevenue() {
    return payments.sumDeliveredPaidAmount();
  }
}
