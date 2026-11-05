package com.bakery.cart.controller.web;

import com.bakery.cart.service.impl.SessionCartServiceImpl;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {
  private final SessionCartServiceImpl carts;

  @GetMapping
  public String view(HttpSession session, Model model) {
    model.addAttribute("cart", carts.getCart(session));
    return "cart/view";
  }
}
