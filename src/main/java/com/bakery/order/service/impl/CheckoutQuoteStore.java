package com.bakery.order.service.impl;

import com.bakery.cart.dto.CartResponse;
import com.bakery.cart.model.Cart;
import com.bakery.common.exception.ConflictException;
import jakarta.servlet.http.HttpSession;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.util.WebUtils;

/** Server-side prices and quantities the customer actually reviewed, bounded per session. */
@Service
public class CheckoutQuoteStore {

  private static final String ATTRIBUTE = CheckoutQuoteStore.class.getName() + ".quotes";
  private static final int MAX_QUOTES = 8;

  public String issue(Long accountId, HttpSession session, CartResponse cart) {
    synchronized (WebUtils.getSessionMutex(session)) {
      Map<Long, Line> lines = new LinkedHashMap<>();
      cart.getItems().forEach(item -> lines.put(item.getProductId(),
          new Line(item.getQuantity(), item.getUnitPrice())));
      String token = UUID.randomUUID().toString();
      var quotes = quotes(session);
      quotes.put(token, new Quote(accountId, lines));
      while (quotes.size() > MAX_QUOTES) quotes.remove(quotes.keySet().iterator().next());
      session.setAttribute(ATTRIBUTE, quotes);
      return token;
    }
  }

  public void verify(Long accountId, HttpSession session, String token, Cart cart) {
    synchronized (WebUtils.getSessionMutex(session)) {
      Quote quote = quotes(session).get(token);
      if (quote == null || !accountId.equals(quote.accountId())) {
        throw new ConflictException("Phiên đặt hàng đã thay đổi. Vui lòng kiểm tra lại giỏ bánh và xác nhận lại.");
      }
      if (cart == null || cart.getItems().isEmpty() || quote.lines().size() != cart.getItems().size()) {
        throw new ConflictException("Giỏ bánh đã thay đổi. Vui lòng kiểm tra lại trước khi đặt hàng.");
      }
      cart.getItems().forEach((id, item) -> {
        Line line = quote.lines().get(id);
        if (line == null || line.quantity() != item.getQuantity() || line.price() == null
            || item.getUnitPrice() == null || line.price().compareTo(item.getUnitPrice()) != 0) {
          throw new ConflictException("Giá hoặc số lượng bánh đã thay đổi. Vui lòng xem lại giỏ và xác nhận lại.");
        }
      });
    }
  }

  public static String orderCode(String token) {
    try {
      var uuid = UUID.fromString(token);
      if (!uuid.toString().equalsIgnoreCase(token)) throw new IllegalArgumentException();
      // The full UUID fits the existing 30-character unique order code column in base 36.
      return "LN" + new BigInteger(uuid.toString().replace("-", ""), 16)
          .toString(36).toUpperCase(Locale.ROOT);
    } catch (IllegalArgumentException | NullPointerException invalid) {
      throw new ConflictException("Phiên đặt hàng không hợp lệ. Vui lòng mở lại trang thanh toán.");
    }
  }

  @SuppressWarnings("unchecked")
  private LinkedHashMap<String, Quote> quotes(HttpSession session) {
    var existing = session.getAttribute(ATTRIBUTE);
    return existing instanceof LinkedHashMap<?, ?>
        ? new LinkedHashMap<>((LinkedHashMap<String, Quote>) existing) : new LinkedHashMap<>();
  }

  private record Line(int quantity, BigDecimal price) implements Serializable {}
  private record Quote(Long accountId, Map<Long, Line> lines) implements Serializable {}
}
