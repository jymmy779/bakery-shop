package com.bakery.catalog.controller.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bakery.account.entity.Account;
import com.bakery.account.repository.AccountRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CmsSidebarAdviceTest {
  private final AccountRepository accounts = mock(AccountRepository.class);
  private final CmsSidebarAdvice advice = new CmsSidebarAdvice(accounts);

  @Test
  void showsFullNameOfLoggedInAccount() {
    Account account = new Account();
    account.setFullName("Nguyễn Trần Thái Sơn");
    when(accounts.findByEmailIgnoreCase("admin@bakery.com")).thenReturn(Optional.of(account));

    assertEquals("Nguyễn Trần Thái Sơn", advice.cmsDisplayName(() -> "admin@bakery.com"));
  }

  @Test
  void fallsBackToEmailWhenAccountHasNoName() {
    when(accounts.findByEmailIgnoreCase("staff@bakery.com")).thenReturn(Optional.empty());

    assertEquals("staff@bakery.com", advice.cmsDisplayName(() -> "staff@bakery.com"));
  }
}
