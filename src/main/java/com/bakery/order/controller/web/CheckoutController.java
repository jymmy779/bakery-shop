package com.bakery.order.controller.web;

import com.bakery.account.security.CustomUserDetails;
import com.bakery.common.exception.BusinessException;
import com.bakery.order.dto.CheckoutRequest;
import com.bakery.order.service.impl.CheckoutPageServiceImpl;
import com.bakery.order.service.impl.CheckoutResultServiceImpl;
import com.bakery.order.service.impl.CheckoutSubmissionServiceImpl;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;

@Controller
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class CheckoutController {

  private final CheckoutPageServiceImpl checkoutPages;
  private final CheckoutSubmissionServiceImpl submissions;
  private final CheckoutResultServiceImpl results;

  @InitBinder("checkoutRequest")
  public void bindCheckout(WebDataBinder binder) {
    binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    binder.setAllowedFields("checkoutToken", "receiverName", "receiverPhone", "receiverAddress",
        "receiverEmail", "deliveryDate", "deliveryTime", "note", "productNotes[*]");
  }

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

  @PostMapping
  public String placeOrder(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @ModelAttribute("checkoutRequest") CheckoutRequest request,
      BindingResult errors, HttpSession session, Model model) {
    if (!errors.hasErrors()) {
      try {
        var order = submissions.submit(userDetails.getAccountId(), session, request);
        return "redirect:/checkout/success/" + order.getId();
      } catch (BusinessException failure) {
        model.addAttribute("checkoutError", failure.getMessage());
      }
    } else {
      model.addAttribute("checkoutError", "Vui lòng kiểm tra các thông tin được đánh dấu bên dưới.");
    }
    var page = checkoutPages.prepare(userDetails.getAccountId(), session, null);
    request.setCheckoutToken(page.getRequest().getCheckoutToken());
    model.addAttribute("checkout", page);
    return "order/checkout";
  }

  @GetMapping("/success/{orderId}")
  public String success(@AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long orderId, Model model) {
    model.addAttribute("order", results.getForCustomer(userDetails.getAccountId(), orderId));
    return "order/checkout-success";
  }
}
