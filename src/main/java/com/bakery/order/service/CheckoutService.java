package com.bakery.order.service;

import com.bakery.cart.model.Cart;
import com.bakery.order.dto.CheckoutRequest;
import com.bakery.order.dto.OrderDetailResponse;

public interface CheckoutService {

  OrderDetailResponse checkout(Long accountId, Cart cart, CheckoutRequest request);
}
