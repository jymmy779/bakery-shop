package com.bakery.catalog.controller.admin;

import com.bakery.account.entity.Account;
import com.bakery.account.repository.AccountRepository;
import java.security.Principal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackages = {
    "com.bakery.account.controller.admin",
    "com.bakery.catalog.controller.admin",
    "com.bakery.dashboard.controller.admin",
    "com.bakery.order.controller.admin"
})
public class CmsSidebarAdvice {
  private final AccountRepository accounts;

  public CmsSidebarAdvice(AccountRepository accounts) {
    this.accounts = accounts;
  }

  @ModelAttribute("cmsDisplayName")
  public String cmsDisplayName(Principal principal) {
    if (principal == null) return "";
    return accounts.findByEmailIgnoreCase(principal.getName())
        .map(Account::getFullName)
        .filter(StringUtils::hasText)
        .orElse(principal.getName());
  }
}
