package com.bakery.order.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.bakery.cart.dto.CartItemResponse;
import com.bakery.cart.dto.CartResponse;
import com.bakery.cart.model.Cart;
import com.bakery.cart.model.CartItem;
import com.bakery.common.exception.ConflictException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

class CheckoutQuoteStoreTest {

  private final CheckoutQuoteStore quotes = new CheckoutQuoteStore();
  private final MockHttpSession session = new MockHttpSession();

  @Test
  void verifiesOnlyTheReviewedPricesQuantitiesAndOwner() {
    String token = quotes.issue(7L, session, view());
    assertDoesNotThrow(() -> quotes.verify(7L, session, token, cart()));
    assertThrows(ConflictException.class, () -> quotes.verify(8L, session, token, cart()));
    var changedPrice = cart();
    changedPrice.getItems().get(42L).setUnitPrice(new BigDecimal("450001"));
    assertThrows(ConflictException.class, () -> quotes.verify(7L, session, token, changedPrice));
    var changedQuantity = cart();
    changedQuantity.getItems().get(42L).setQuantity(3);
    assertThrows(ConflictException.class, () -> quotes.verify(7L, session, token, changedQuantity));
    assertThrows(ConflictException.class, () -> quotes.verify(7L, session, token, new Cart()));
  }

  @Test
  void tokenDoesNotCarryAcrossSessionsAndOldestQuotesAreEvicted() {
    String token = quotes.issue(7L, session, view());
    assertThrows(ConflictException.class,
        () -> quotes.verify(7L, new MockHttpSession(), token, cart()));
    for (int index = 0; index < 8; index++) quotes.issue(7L, session, view());
    assertThrows(ConflictException.class, () -> quotes.verify(7L, session, token, cart()));
  }

  @Test
  void fullTokenMapsDeterministicallyToExistingUniqueOrderCodeColumn() {
    String token = UUID.randomUUID().toString();
    String code = CheckoutQuoteStore.orderCode(token);
    assertEquals(code, CheckoutQuoteStore.orderCode(token));
    assertTrue(code.length() <= 30);
    assertNotEquals(code, CheckoutQuoteStore.orderCode(UUID.randomUUID().toString()));
    assertThrows(ConflictException.class, () -> CheckoutQuoteStore.orderCode("1-1-1-1-1"));
  }

  private CartResponse view() {
    var item = new CartItemResponse();
    item.setProductId(42L);
    item.setUnitPrice(new BigDecimal("450000"));
    item.setQuantity(2);
    var cart = new CartResponse();
    cart.setItems(List.of(item));
    return cart;
  }

  private Cart cart() {
    var item = new CartItem();
    item.setProductId(42L);
    item.setUnitPrice(new BigDecimal("450000.00"));
    item.setQuantity(2);
    var cart = new Cart();
    cart.getItems().put(42L, item);
    return cart;
  }
}
