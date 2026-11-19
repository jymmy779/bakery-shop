package com.bakery.order.service.impl;

import com.bakery.account.dto.AccountAddressResponse;
import com.bakery.account.service.AccountAddressService;
import com.bakery.account.service.AccountService;
import com.bakery.cart.service.impl.SessionCartServiceImpl;
import com.bakery.order.dto.CheckoutPageResponse;
import com.bakery.order.dto.CheckoutRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Prepares the checkout screen without placing an order or modifying the cart. */
@Service
@RequiredArgsConstructor
public class CheckoutPageServiceImpl {

  private static final ZoneId SHOP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
  private final SessionCartServiceImpl carts;
  private final AccountService accounts;
  private final AccountAddressService addresses;
  private final CheckoutQuoteStore quotes;

  public CheckoutPageResponse prepare(Long accountId, HttpSession session, Long addressId) {
    var cart = carts.getCart(session);
    var profile = accounts.getById(accountId);
    // Only addresses belonging to the authenticated account can populate the form.
    var savedAddresses = addresses.findByAccountId(accountId);
    var selected = addressId == null
        ? savedAddresses.stream().filter(AccountAddressResponse::isDefaultAddress)
            .findFirst().orElse(savedAddresses.isEmpty() ? null : savedAddresses.get(0))
        : savedAddresses.stream().filter(address -> addressId.equals(address.getId()))
            .findFirst().orElse(null);
    var earliestDate = LocalDate.now(SHOP_ZONE)
        .plusDays(Math.max(0, cart.getMaximumPreparationDays()));

    var request = new CheckoutRequest();
    request.setCheckoutToken(quotes.issue(accountId, session, cart));
    request.setReceiverName(selected == null ? profile.getFullName() : selected.getReceiverName());
    request.setReceiverPhone(selected == null ? profile.getPhone() : selected.getReceiverPhone());
    request.setReceiverAddress(selected == null ? profile.getAddress() : selected.getAddress());
    request.setReceiverEmail(profile.getEmail());
    request.setDeliveryDate(earliestDate);
    request.setNote(selected == null ? null : selected.getDeliveryNote());

    var page = new CheckoutPageResponse();
    page.setCart(cart);
    page.setRequest(request);
    page.setSavedAddresses(savedAddresses);
    page.setSelectedAddressId(selected == null ? null : selected.getId());
    page.setEarliestDeliveryDate(earliestDate);
    page.setAddressUnavailable(addressId != null && selected == null);
    return page;
  }
}
