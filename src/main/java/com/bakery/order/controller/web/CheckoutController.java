package com.bakery.order.controller.web;

import com.bakery.order.dto.CheckoutRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

  @GetMapping
  public String checkout(Model model) {
    model.addAttribute("checkoutRequest", new CheckoutRequest());
    return "order/checkout";
  }
}
