package com.bakery.cart.service.impl;

import com.bakery.cart.dto.AddToCartRequest;
import com.bakery.cart.dto.CartResponse;
import com.bakery.cart.dto.UpdateCartItemRequest;
import com.bakery.cart.model.Cart;
import com.bakery.cart.model.CartItem;
import com.bakery.cart.service.CartService;
import com.bakery.catalog.entity.ProductStatus;
import com.bakery.catalog.service.ProductQueryService;
import com.bakery.common.exception.ConflictException;
import com.bakery.common.exception.InvalidOperationException;
import com.bakery.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartServiceImpl implements CartService {
  private final ProductQueryService products;
  private final CartQuantityServiceImpl quantities;

  @Override
  public Cart addItem(Cart cart, AddToCartRequest request) {
    if (request == null || request.getProductId() == null || request.getQuantity() == null
        || request.getQuantity() < 1) {
      throw new InvalidOperationException("Mã sản phẩm và số lượng từ 1 trở lên là bắt buộc");
    }
    Cart result = cart == null ? new Cart() : cart;
    synchronized (result) {
      var product = products.getById(request.getProductId());
      CartItem existing = result.getItems().get(request.getProductId());
      long quantity = (long) request.getQuantity() + (existing == null ? 0 : existing.getQuantity());
      if (product.getStatus() != ProductStatus.SELLING) {
        throw new ConflictException("Sản phẩm đã ngừng bán");
      }
      if (quantity > product.getStockQuantity()) {
        throw new ConflictException("Số lượng vượt tồn kho còn lại: " + product.getStockQuantity());
      }
      CartItem item = existing == null ? new CartItem() : existing;
      item.setProductId(product.getId());
      item.setProductName(product.getName());
      item.setQuantity((int) quantity);
      item.setUnitPrice(product.getPrice());
      item.setAvailableStock(product.getStockQuantity());
      item.setPreparationDays(product.getPreparationDays());
      item.setImagePath(product.getImages().isEmpty() ? null : product.getImages().get(0).getImagePath());
      result.getItems().put(request.getProductId(), item);
      return result;
    }
  }

  @Override
  public Cart updateItem(Cart cart, Long productId, UpdateCartItemRequest request) {
    quantities.updateItem(cart, productId, request);
    return cart;
  }

  @Override
  public Cart removeItem(Cart cart, Long productId) {
    if (cart == null) {
      throw new ResourceNotFoundException("Sản phẩm không có trong giỏ hàng");
    }
    synchronized (cart) {
      if (cart.getItems().remove(productId) == null) {
        throw new ResourceNotFoundException("Sản phẩm không có trong giỏ hàng");
      }
      return cart;
    }
  }

  @Override
  public Cart refresh(Cart cart) {
    if (cart == null) {
      return new Cart();
    }
    synchronized (cart) {
      // Validate all lines first so a failed refresh never partially changes the cart.
      Cart refreshed = new Cart();
      for (CartItem item : cart.getItems().values()) {
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(item.getProductId());
        request.setQuantity(item.getQuantity());
        addItem(refreshed, request);
      }
      cart.setItems(refreshed.getItems());
      return cart;
    }
  }

  @Override
  public CartResponse getCart(Cart cart) {
    return quantities.getCart(cart);
  }
}
