package com.bakery.order.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/orders")
public class OrderController {

  @GetMapping
  public String list() {
    return "order/list";
  }

  @GetMapping("/{orderId}")
  public String detail(@PathVariable Long orderId, Model model) {
    model.addAttribute("orderId", orderId);
    return "order/detail";
  }
}
