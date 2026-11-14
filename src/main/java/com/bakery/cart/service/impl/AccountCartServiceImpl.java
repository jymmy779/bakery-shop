package com.bakery.cart.service.impl;

import com.bakery.account.entity.Account;
import com.bakery.account.repository.AccountRepository;
import com.bakery.cart.dto.AddToCartRequest;
import com.bakery.cart.dto.CartResponse;
import com.bakery.cart.dto.UpdateCartItemRequest;
import com.bakery.cart.entity.AccountCart;
import com.bakery.cart.entity.AccountCartItem;
import com.bakery.cart.model.Cart;
import com.bakery.cart.model.CartItem;
import com.bakery.cart.repository.AccountCartRepository;
import com.bakery.catalog.entity.Product;
import com.bakery.catalog.repository.ProductRepository;
import com.bakery.cart.service.CartService;
import com.bakery.catalog.service.ProductQueryService;
import com.bakery.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountCartServiceImpl {

  private final AccountCartRepository carts;
  private final AccountRepository accounts;
  private final ProductQueryService products;
  private final ProductRepository productRepository;
  private final CartService cartRules;

  @Transactional(readOnly = true)
  public CartResponse getCart(Long accountId) {
    return cartRules.getCart(toModel(carts.findByAccount_Id(accountId).orElse(null)));
  }

  @Transactional
  public Cart getModel(Long accountId) {
    // Checkout must lock before reading items; the surrounding transaction also creates the order.
    return toModel(carts.findForCheckout(accountId).orElse(null));
  }

  @Transactional
  public CartResponse addItem(Long accountId, AddToCartRequest request) {
    AccountCart entity = getOrCreate(accountId);
    Cart cart = cartRules.addItem(toModel(entity), request);
    replaceItems(entity, cart);
    return cartRules.getCart(cart);
  }

  @Transactional
  public CartResponse updateItem(Long accountId, Long productId, UpdateCartItemRequest request) {
    AccountCart entity = findCartForUpdate(accountId)
        .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không có trong giỏ hàng"));
    Cart cart = cartRules.updateItem(toModel(entity), productId, request);
    replaceItems(entity, cart);
    return cartRules.getCart(cart);
  }

  @Transactional
  public CartResponse removeItem(Long accountId, Long productId) {
    AccountCart entity = findCartForUpdate(accountId)
        .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không có trong giỏ hàng"));
    Cart cart = cartRules.removeItem(toModel(entity), productId);
    replaceItems(entity, cart);
    return cartRules.getCart(cart);
  }

  @Transactional
  public void mergeGuestCart(Long accountId, Cart guestCart) {
    if (guestCart == null || guestCart.getItems().isEmpty()) {
      return;
    }
    AccountCart entity = getOrCreate(accountId);
    Cart merged = toModel(entity);
    for (CartItem guestItem : guestCart.getItems().values()) {
      try {
        products.getById(guestItem.getProductId());
      } catch (ResourceNotFoundException missingProduct) {
        continue;
      }
      CartItem existing = merged.getItems().get(guestItem.getProductId());
      long quantity = (long) guestItem.getQuantity() + (existing == null ? 0 : existing.getQuantity());
      if (quantity > Integer.MAX_VALUE) {
        throw new IllegalStateException("Tổng số lượng sản phẩm trong giỏ vượt giới hạn");
      }
      CartItem line = existing == null ? new CartItem() : existing;
      line.setProductId(guestItem.getProductId());
      line.setQuantity((int) quantity);
      merged.getItems().put(line.getProductId(), line);
    }
    replaceItems(entity, merged);
  }

  @Transactional
  public void clear(Long accountId) {
    findCartForUpdate(accountId).ifPresent(cart -> {
      cart.getItems().clear();
      carts.save(cart);
    });
  }

  private java.util.Optional<AccountCart> findCartForUpdate(Long accountId) {
    return carts.findWithItemsByAccount_Id(accountId);
  }

  private AccountCart getOrCreate(Long accountId) {
    return findCartForUpdate(accountId).orElseGet(() -> {
      Account account = accounts.findById(accountId)
          .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
      AccountCart cart = new AccountCart();
      cart.setAccount(account);
      return carts.save(cart);
    });
  }

  private Cart toModel(AccountCart entity) {
    Cart result = new Cart();
    if (entity == null) {
      return result;
    }
    entity.getItems().forEach(item -> {
      var product = products.getById(item.getProduct().getId());
      CartItem line = new CartItem();
      line.setProductId(product.getId());
      line.setProductName(product.getName());
      line.setImagePath(product.getImages().isEmpty() ? null : product.getImages().get(0).getImagePath());
      line.setUnitPrice(product.getPrice());
      line.setQuantity(item.getQuantity());
      line.setAvailableStock(product.getStockQuantity());
      line.setPreparationDays(product.getPreparationDays());
      result.getItems().put(product.getId(), line);
    });
    return result;
  }

  private void replaceItems(AccountCart entity, Cart model) {
    var remaining = new java.util.HashMap<Long, AccountCartItem>();
    entity.getItems().forEach(item -> remaining.put(item.getProduct().getId(), item));
    entity.getItems().removeIf(item -> !model.getItems().containsKey(item.getProduct().getId()));
    model.getItems().values().forEach(line -> {
      AccountCartItem item = remaining.get(line.getProductId());
      if (item == null) {
        item = new AccountCartItem();
        item.setCart(entity);
        Product product = productRepository.getReferenceById(line.getProductId());
        item.setProduct(product);
        entity.getItems().add(item);
      }
      item.setQuantity(line.getQuantity());
    });
    carts.save(entity);
  }
}
