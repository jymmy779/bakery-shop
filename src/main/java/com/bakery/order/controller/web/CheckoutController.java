package com.bakery.order.controller.web;

import com.bakery.account.security.CustomUserDetails;
import com.bakery.order.service.impl.CheckoutPageServiceImpl;
import jakarta.servlet.http.HttpSession;
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

  private final CheckoutPageServiceImpl checkoutPages;

  @GetMapping
  public String checkout(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestParam(name = "addressId", required = false) Long addressId,
      HttpSession session,
      Model model) {
    var page = checkoutPages.prepare(userDetails.getAccountId(), session, addressId);
    model.addAttribute("checkout", page);
    model.addAttribute("checkoutRequest", page.getRequest());
    return "order/checkout";
  }
}
