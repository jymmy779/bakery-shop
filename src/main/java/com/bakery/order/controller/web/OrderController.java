package com.bakery.order.controller.web;

import com.bakery.account.security.CustomUserDetails;
import com.bakery.order.entity.OrderStatus;
import com.bakery.order.service.impl.CustomerOrderQueryServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

  private final CustomerOrderQueryServiceImpl orders;

  @GetMapping
  public String list(@AuthenticationPrincipal CustomUserDetails customer,
      @RequestParam(required = false) OrderStatus status,
      @RequestParam(defaultValue = "0") int page, Model model) {
    model.addAttribute("orders", orders.findCustomerOrders(customer.getAccountId(), status, page));
    model.addAttribute("selectedStatus", status);
    return "order/list";
  }

  @GetMapping("/{orderId}")
  public String detail(@AuthenticationPrincipal CustomUserDetails customer,
      @PathVariable Long orderId, Model model) {
    model.addAttribute("order", orders.getCustomerOrder(customer.getAccountId(), orderId));
    return "order/detail";
  }
}
