package com.bakery.cart.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.bakery.cart.dto.UpdateCartItemRequest;
import com.bakery.cart.model.Cart;
import com.bakery.cart.model.CartItem;
import com.bakery.catalog.dto.ProductDetailResponse;
import com.bakery.catalog.entity.ProductStatus;
import com.bakery.catalog.service.ProductQueryService;
import com.bakery.common.exception.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CartQuantityServiceImplTest {
  private final ProductQueryService products = mock(ProductQueryService.class);
  private final CartQuantityServiceImpl service = new CartQuantityServiceImpl(products);
  private Cart cart;

  @BeforeEach
  void setup() {
    cart = new Cart();
    cart.getItems().put(1L, item(1L, 2, "10000"));
    cart.getItems().put(2L, item(2L, 1, "20000"));
  }

  @Test
  void recalculatesAllTotalsUsingFreshPriceAndAllowsExactStock() {
    product(3, ProductStatus.SELLING);
    var result = service.updateItem(cart, 1L, request(3));
    assertEquals(new BigDecimal("36000"), result.getItems().get(0).getLineTotal());
    assertEquals(new BigDecimal("56000"), result.getTotalAmount());
    assertEquals(4, result.getTotalQuantity());
    assertEquals(3, result.getItems().get(0).getAvailableStock());
  }

  @Test
  void allowsDecreaseToOne() {
    product(5, ProductStatus.SELLING);
    assertEquals(1, service.updateItem(cart, 1L, request(1)).getItems().get(0).getQuantity());
  }

  @Test
  void rejectsInvalidQuantitiesWithoutMutatingCart() {
    for (Integer quantity : new Integer[]{null, 0, -1}) {
      assertThrows(InvalidOperationException.class, () -> service.updateItem(cart, 1L, request(quantity)));
    }
    assertEquals(2, cart.getItems().get(1L).getQuantity());
    verifyNoInteractions(products);
  }

  @Test
  void rejectsStockExceededAndSoldOutWithoutMutation() {
    for (int stock : new int[]{1, 0}) {
      product(stock, ProductStatus.SELLING);
      assertThrows(ConflictException.class, () -> service.updateItem(cart, 1L, request(3)));
      assertEquals(2, cart.getItems().get(1L).getQuantity());
      assertEquals(new BigDecimal("10000"), cart.getItems().get(1L).getUnitPrice());
    }
  }

  @Test
  void rejectsStoppedProduct() {
    product(5, ProductStatus.STOPPED);
    assertThrows(ConflictException.class, () -> service.updateItem(cart, 1L, request(1)));
  }

  @Test
  void rejectsAbsentSessionOrItem() {
    assertThrows(ResourceNotFoundException.class, () -> service.updateItem(null, 1L, request(1)));
    assertThrows(ResourceNotFoundException.class, () -> service.updateItem(cart, 99L, request(1)));
    verifyNoInteractions(products);
  }

  private void product(int stock, ProductStatus status) {
    var product = new ProductDetailResponse();
    product.setPrice(new BigDecimal("12000"));
    product.setStockQuantity(stock);
    product.setStatus(status);
    when(products.getById(1L)).thenReturn(product);
  }

  private UpdateCartItemRequest request(Integer quantity) {
    var request = new UpdateCartItemRequest();
    request.setQuantity(quantity);
    return request;
  }

  private CartItem item(Long id, int quantity, String price) {
    var item = new CartItem();
    item.setProductId(id);
    item.setQuantity(quantity);
    item.setUnitPrice(new BigDecimal(price));
    return item;
  }
}
