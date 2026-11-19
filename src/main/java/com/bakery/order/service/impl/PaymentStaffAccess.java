package com.bakery.order.service.impl;

import com.bakery.account.dto.AccountProfileResponse;
import com.bakery.account.entity.AccountStatus;
import com.bakery.account.entity.Role;
import com.bakery.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentStaffAccess {

  private final AccountService accounts;

  public AccountProfileResponse requireActiveStaff(Long accountId) {
    var account = accounts.getById(accountId);
    if (account.getStatus() != AccountStatus.ACTIVE
        || (account.getRole() != Role.ROLE_STAFF && account.getRole() != Role.ROLE_ADMIN)) {
      throw new AccessDeniedException("Tài khoản không được phép đối soát thanh toán.");
    }
    return account;
  }
}
