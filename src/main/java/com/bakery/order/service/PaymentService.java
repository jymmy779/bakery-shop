package com.bakery.order.service;

import com.bakery.order.dto.PaymentConfirmationRequest;
import com.bakery.order.dto.PaymentResponse;

public interface PaymentService {

  PaymentResponse confirmBankTransfer(
      Long orderId,
      Long confirmedByAccountId,
      PaymentConfirmationRequest request);
}
