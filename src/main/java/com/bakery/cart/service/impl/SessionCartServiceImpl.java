package com.bakery.cart.service.impl;

import com.bakery.cart.dto.AddToCartRequest;
import com.bakery.cart.dto.CartResponse;
import com.bakery.cart.dto.UpdateCartItemRequest;
import com.bakery.cart.model.Cart;
import com.bakery.cart.service.CartService;
import jakarta.servlet.http.HttpSession;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.util.WebUtils;

@Service
@RequiredArgsConstructor
public class SessionCartServiceImpl {
  public static final String CART_ATTRIBUTE = "cart";
  private final CartService carts;

  public CartResponse getCart(HttpSession session) {
    synchronized (WebUtils.getSessionMutex(session)) {
      return carts.getCart(current(session));
    }
  }

  public CartResponse addItem(HttpSession session, AddToCartRequest request) {
    return change(session, cart -> carts.addItem(cart, request));
  }

  public CartResponse updateItem(HttpSession session, Long productId, UpdateCartItemRequest request) {
    return change(session, cart -> carts.updateItem(cart, productId, request));
  }

  public CartResponse removeItem(HttpSession session, Long productId) {
    return change(session, cart -> carts.removeItem(cart, productId));
  }

  private CartResponse change(HttpSession session, Function<Cart, Cart> operation) {
    synchronized (WebUtils.getSessionMutex(session)) {
      Cart cart = operation.apply(current(session));
      session.setAttribute(CART_ATTRIBUTE, cart);
      return carts.getCart(cart);
    }
  }

  private Cart current(HttpSession session) {
    return (Cart) session.getAttribute(CART_ATTRIBUTE);
  }

  /** Called by checkout integration; never expose a client-controlled success/clear endpoint. */
  public <T> T checkout(HttpSession session, Function<Cart, T> placeOrder) {
    synchronized (WebUtils.getSessionMutex(session)) {
      Cart checkedOut = current(session);
      T result = placeOrder.apply(checkedOut);
      if (TransactionSynchronizationManager.isActualTransactionActive()
          && TransactionSynchronizationManager.isSynchronizationActive()) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            clearPurchasedCart(session, checkedOut);
          }
        });
      } else {
        clearPurchasedCart(session, checkedOut);
      }
      return result;
    }
  }

  private void clearPurchasedCart(HttpSession session, Cart checkedOut) {
    synchronized (WebUtils.getSessionMutex(session)) {
      if (current(session) == checkedOut) {
        session.removeAttribute(CART_ATTRIBUTE);
      }
    }
  }
}
