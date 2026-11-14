package com.bakery.order.controller.web;

import com.bakery.account.security.CustomUserDetails;
import com.bakery.common.exception.ConflictException;
import com.bakery.common.exception.InvalidOperationException;
import com.bakery.order.dto.CancelOrderRequest;
import com.bakery.order.service.impl.CustomerOrderCancellationServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class CustomerOrderCancellationController {

  private final CustomerOrderCancellationServiceImpl cancellations;

  @InitBinder("cancellation")
  void bindCancellation(WebDataBinder binder) {
    binder.setAllowedFields("reason");
  }

  @PostMapping("/{orderId}/cancel")
  public String cancel(@AuthenticationPrincipal CustomUserDetails customer, @PathVariable Long orderId,
      @Valid @ModelAttribute("cancellation") CancelOrderRequest request, BindingResult binding,
      RedirectAttributes redirect) {
    if (binding.hasErrors()) {
      redirect.addFlashAttribute("cancellation", request);
      redirect.addFlashAttribute(BindingResult.MODEL_KEY_PREFIX + "cancellation", binding);
      redirect.addFlashAttribute("cancelErrorMessage", "Vui lòng kiểm tra lý do hủy đơn.");
    } else {
      try {
        cancellations.cancel(customer.getAccountId(), orderId, request);
        redirect.addFlashAttribute("cancelSuccessMessage", "Hủy đơn hàng thành công.");
      } catch (ConflictException | InvalidOperationException exception) {
        redirect.addFlashAttribute("cancelErrorMessage", exception.getMessage());
      }
    }
    return "redirect:/orders/" + orderId;
  }
}
