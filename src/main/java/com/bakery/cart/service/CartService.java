package com.bakery.cart.service;

import com.bakery.cart.dto.AddToCartRequest;
import com.bakery.cart.dto.CartResponse;
import com.bakery.cart.dto.UpdateCartItemRequest;
import com.bakery.cart.model.Cart;

public interface CartService {

  Cart addItem(Cart cart, AddToCartRequest request);

  Cart updateItem(Cart cart, Long productId, UpdateCartItemRequest request);

  Cart removeItem(Cart cart, Long productId);

  Cart refresh(Cart cart);

  CartResponse getCart(Cart cart);
}
