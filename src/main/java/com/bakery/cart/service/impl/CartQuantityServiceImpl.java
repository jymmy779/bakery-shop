package com.bakery.cart.service.impl;

import com.bakery.cart.dto.CartItemResponse;
import com.bakery.cart.dto.CartResponse;
import com.bakery.cart.dto.UpdateCartItemRequest;
import com.bakery.cart.model.Cart;
import com.bakery.cart.model.CartItem;
import com.bakery.catalog.entity.ProductStatus;
import com.bakery.catalog.service.ProductQueryService;
import com.bakery.common.exception.ConflictException;
import com.bakery.common.exception.InvalidOperationException;
import com.bakery.common.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Quantity validation and cart response mapping shared by CartServiceImpl. */
@Service
@RequiredArgsConstructor
public class CartQuantityServiceImpl {

  private final ProductQueryService products;

  @Transactional(readOnly = true)
  public CartResponse updateItem(Cart cart, Long productId, UpdateCartItemRequest request) {
    if (request == null || request.getQuantity() == null || request.getQuantity() < 1) {
      throw new InvalidOperationException("Số lượng phải từ 1 trở lên; dùng nút xóa để bỏ sản phẩm");
    }
    if (cart == null) {
      throw new ResourceNotFoundException("Sản phẩm không có trong giỏ hàng");
    }
    synchronized (cart) {
      CartItem item = cart.getItems().get(productId);
      if (item == null) {
        throw new ResourceNotFoundException("Sản phẩm không có trong giỏ hàng");
      }
      var product = products.getById(productId);
      if (product.getStatus() != ProductStatus.SELLING) {
        throw new ConflictException("Sản phẩm đã ngừng bán");
      }
      if (request.getQuantity() > product.getStockQuantity()) {
        throw new ConflictException("Số lượng vượt tồn kho còn lại: " + product.getStockQuantity());
      }
      item.setQuantity(request.getQuantity());
      item.setUnitPrice(product.getPrice());
      item.setAvailableStock(product.getStockQuantity());
      item.setProductName(product.getName());
      item.setPreparationDays(product.getPreparationDays());
      return toResponse(cart);
    }
  }

  private CartResponse toResponse(Cart cart) {
    CartResponse response = new CartResponse();
    BigDecimal total = BigDecimal.ZERO;
    int quantity = 0;
    int preparationDays = 0;
    for (CartItem item : cart.getItems().values()) {
      CartItemResponse line = new CartItemResponse();
      line.setProductId(item.getProductId());
      line.setProductName(item.getProductName());
      line.setImagePath(item.getImagePath());
      line.setUnitPrice(item.getUnitPrice());
      line.setQuantity(item.getQuantity());
      line.setAvailableStock(item.getAvailableStock());
      line.setPreparationDays(item.getPreparationDays());
      line.setLineTotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
      response.getItems().add(line);
      total = total.add(line.getLineTotal());
      quantity += item.getQuantity();
      preparationDays = Math.max(preparationDays, item.getPreparationDays());
    }
    response.setTotalAmount(total);
    response.setTotalQuantity(quantity);
    response.setMaximumPreparationDays(preparationDays);
    return response;
  }

  public CartResponse getCart(Cart cart) {
    if (cart == null) {
      return toResponse(new Cart());
    }
    synchronized (cart) {
      CartResponse response = toResponse(cart);
      BigDecimal total = BigDecimal.ZERO;
      int preparationDays = 0;
      for (CartItemResponse line : response.getItems()) {
        try {
          var product = products.getById(line.getProductId());
          line.setProductName(product.getName());
          line.setUnitPrice(product.getPrice());
          line.setAvailableStock(product.getStockQuantity());
          line.setPreparationDays(product.getPreparationDays());
          line.setImagePath(product.getImages().isEmpty() ? null : product.getImages().get(0).getImagePath());
          line.setLineTotal(product.getPrice().multiply(BigDecimal.valueOf(line.getQuantity())));
          if (product.getStatus() != ProductStatus.SELLING) {
            line.setAvailabilityMessage("Sản phẩm đã ngừng bán. Vui lòng xóa khỏi giỏ hàng");
          } else if (product.getStockQuantity() < 1) {
            line.setAvailabilityMessage("Sản phẩm tạm hết hàng. Vui lòng xóa khỏi giỏ hàng");
          } else if (line.getQuantity() > product.getStockQuantity()) {
            line.setAvailabilityMessage("Chỉ còn " + product.getStockQuantity() + " sản phẩm. Vui lòng giảm số lượng");
          }
        } catch (ResourceNotFoundException exception) {
          line.setAvailabilityMessage("Sản phẩm không còn tồn tại. Vui lòng xóa khỏi giỏ hàng");
          line.setAvailableStock(0);
        }
        line.setAvailable(line.getAvailabilityMessage() == null);
        total = total.add(line.getLineTotal());
        preparationDays = Math.max(preparationDays, line.getPreparationDays());
      }
      response.setTotalAmount(total);
      response.setMaximumPreparationDays(preparationDays);
      response.setCheckoutAllowed(!response.getItems().isEmpty()
          && response.getItems().stream().allMatch(CartItemResponse::isAvailable));
      return response;
    }
  }
}
