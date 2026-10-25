package com.bakery.account.controller.admin;

import com.bakery.account.dto.AccountStatusUpdateRequest;
import com.bakery.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.data.domain.Pageable;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cms")
@RequiredArgsConstructor
public class AdminAccountController {

  private final AccountService accountService;

  @GetMapping("/accounts")
  public String accounts(Pageable pageable, Model model) {
    model.addAttribute("accounts", accountService.findAll(pageable));
    return "cms/account/list";
  }

  @PostMapping("/accounts/{id}/status")
  public String updateStatus(
      @PathVariable Long id,
      @Valid AccountStatusUpdateRequest request,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
      redirectAttributes.addFlashAttribute("errorMessage", "Trạng thái tài khoản không hợp lệ.");
      return "redirect:/cms/accounts";
    }

    try {
      accountService.updateStatus(id, request);
      redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái tài khoản.");
    } catch (RuntimeException exception) {
      redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
    }
    return "redirect:/cms/accounts";
  }

  @GetMapping("/staff")
  public String staff() {
    return "cms/staff/list";
  }
}
