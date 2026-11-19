package com.bakery.order.service.impl;

import com.bakery.cart.service.impl.SessionCartServiceImpl;
import com.bakery.common.exception.ConflictException;
import com.bakery.order.dto.CheckoutRequest;
import com.bakery.order.dto.OrderDetailResponse;
import com.bakery.order.service.CheckoutService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.web.util.WebUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutSubmissionServiceImpl {

  private final SessionCartServiceImpl carts;
  private final CheckoutService checkout;
  private final CheckoutQuoteStore quotes;
  private final CheckoutResultServiceImpl results;

  public OrderDetailResponse submit(Long accountId, HttpSession session, CheckoutRequest request) {
    synchronized (WebUtils.getSessionMutex(session)) {
      var submitted = results.findSubmitted(accountId, request.getCheckoutToken());
      if (submitted.isPresent()) return submitted.get();
      try {
        // The cart helper opens the transaction and clears only after order creation succeeds.
        return carts.checkout(session, cart -> {
          quotes.verify(accountId, session, request.getCheckoutToken(), cart);
          return checkout.checkout(accountId, cart, request);
        });
      } catch (DataAccessException databaseFailure) {
        log.warn("Checkout transaction failed for account {}", accountId, databaseFailure);
        throw new ConflictException("Chưa thể hoàn tất đặt hàng. Vui lòng thử lại; hệ thống sẽ kiểm tra để tránh tạo đơn trùng.");
      }
    }
  }
}
