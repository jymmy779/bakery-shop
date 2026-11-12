package com.bakery.order.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/cms/transactions")
public class AdminTransactionController {

  @GetMapping
  public String transactions() {
    return "cms/order/transactions";
  }
}
