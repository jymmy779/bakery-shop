package com.bakery.order.controller.admin;

import com.bakery.account.security.CustomUserDetails;
import com.bakery.common.exception.ConflictException;
import com.bakery.common.exception.InvalidOperationException;
import com.bakery.order.dto.PaymentConfirmationRequest;
import com.bakery.order.entity.OrderStatus;
import com.bakery.order.entity.PaymentStatus;
import com.bakery.order.service.PaymentService;
import com.bakery.order.service.impl.AdminOrderQueryServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cms/orders")
@RequiredArgsConstructor
public class AdminOrderController {

  private final AdminOrderQueryServiceImpl orders;
  private final PaymentService payments;

  @InitBinder("confirmation")
  void bindConfirmation(WebDataBinder binder) {
    binder.setAllowedFields("bankReference");
  }

  @GetMapping
  public String orders(@AuthenticationPrincipal CustomUserDetails staff,
      @RequestParam(defaultValue = "") String search,
      @RequestParam(required = false) OrderStatus status,
      @RequestParam(required = false) PaymentStatus paymentStatus,
      @RequestParam(defaultValue = "0") int page, Model model) {
    model.addAttribute("orders", orders.findOrders(staff.getAccountId(), search, status, paymentStatus, page));
    model.addAttribute("search", search.trim());
    model.addAttribute("selectedStatus", status);
    model.addAttribute("selectedPaymentStatus", paymentStatus);
    return "cms/order/list";
  }

  @GetMapping("/{orderId}")
  public String detail(@AuthenticationPrincipal CustomUserDetails staff,
      @PathVariable Long orderId, Model model) {
    var result = orders.getOrder(staff.getAccountId(), orderId);
    model.addAttribute("order", result.order());
    model.addAttribute("canConfirmPayment", result.canConfirmPayment());
    if (!model.containsAttribute("confirmation")) {
      model.addAttribute("confirmation", new PaymentConfirmationRequest());
    }
    return "cms/order/detail";
  }

  @PostMapping("/{orderId}/payment/confirm")
  public String confirm(@AuthenticationPrincipal CustomUserDetails staff, @PathVariable Long orderId,
      @Valid @ModelAttribute("confirmation") PaymentConfirmationRequest request,
      BindingResult binding, RedirectAttributes redirect) {
    if (binding.hasErrors()) {
      redirect.addFlashAttribute("errorMessage", "Mã tham chiếu ngân hàng tối đa 100 ký tự.");
    } else {
      try {
        payments.confirmBankTransfer(orderId, staff.getAccountId(), request);
        redirect.addFlashAttribute("successMessage", "Đã xác nhận thanh toán thành công. Đơn hàng đã được xác nhận.");
      } catch (ConflictException | InvalidOperationException exception) {
        redirect.addFlashAttribute("errorMessage", exception.getMessage());
        redirect.addFlashAttribute("confirmation", request);
      }
    }
    return "redirect:/cms/orders/" + orderId;
  }
}
