package com.bakery.order.controller.admin;

import com.bakery.account.security.CustomUserDetails;
import com.bakery.order.service.impl.AdminOrderQueryServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/cms/transactions")
@RequiredArgsConstructor
public class AdminTransactionController {

  private final AdminOrderQueryServiceImpl orderQueryService;

  @GetMapping
  public String transactions(
      @AuthenticationPrincipal CustomUserDetails staff,
      @RequestParam(defaultValue = "") String search,
      @RequestParam(defaultValue = "0") int page,
      Model model) {
    model.addAttribute("transactions",
        orderQueryService.findTransactions(staff.getAccountId(), search, page));
    model.addAttribute("search", search.trim());
    return "cms/order/transactions";
  }

  @GetMapping("/{id}")
  public String transactionDetail(
      @AuthenticationPrincipal CustomUserDetails staff,
      @PathVariable Long id,
      Model model) {
    model.addAttribute("transaction",
        orderQueryService.getTransactionDetail(staff.getAccountId(), id));
    return "cms/order/transaction-detail";
  }
}

