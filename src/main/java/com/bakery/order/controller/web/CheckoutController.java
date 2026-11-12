package com.bakery.order.controller.web;

import com.bakery.account.security.CustomUserDetails;
import com.bakery.account.service.AccountAddressService;
import com.bakery.order.dto.CheckoutRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Controller
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class CheckoutController {

  private final AccountAddressService accountAddressService;

  @GetMapping
  public String checkout(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestParam(name = "addressId", required = false) Long addressId,
      Model model) {
    model.addAttribute("checkoutRequest", new CheckoutRequest());
    var addresses = accountAddressService.findByAccountId(userDetails.getAccountId());
    model.addAttribute("savedAddresses", addresses);
    model.addAttribute("selectedAddressId", addressId != null ? addressId : addresses.stream()
        .filter(address -> address.isDefaultAddress())
        .map(address -> address.getId())
        .findFirst().orElse(null));
    return "order/checkout";
  }
}
