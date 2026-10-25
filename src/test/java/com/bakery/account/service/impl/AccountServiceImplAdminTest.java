package com.bakery.account.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bakery.account.dto.AccountStatusUpdateRequest;
import com.bakery.account.entity.Account;
import com.bakery.account.entity.AccountStatus;
import com.bakery.account.entity.Role;
import com.bakery.account.repository.AccountRepository;
import com.bakery.common.exception.InvalidOperationException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplAdminTest {

  @Mock AccountRepository accountRepository;
  @Mock PasswordEncoder passwordEncoder;

  private AccountServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new AccountServiceImpl(accountRepository, passwordEncoder);
  }

  @Test
  void findsOnlyCustomerAccountsThroughRoleQuery() {
    Account customer = account(1L, Role.ROLE_CUSTOMER, AccountStatus.ACTIVE);
    var pageable = PageRequest.of(0, 20);
    when(accountRepository.findByRole(Role.ROLE_CUSTOMER, pageable))
        .thenReturn(new PageImpl<>(List.of(customer), pageable, 1));

    var result = service.findAll(pageable);

    assertEquals(1, result.getTotalElements());
    assertEquals(Role.ROLE_CUSTOMER, result.getContent().get(0).getRole());
    verify(accountRepository).findByRole(Role.ROLE_CUSTOMER, pageable);
  }

  @Test
  void locksAndUnlocksCustomer() {
    Account customer = account(1L, Role.ROLE_CUSTOMER, AccountStatus.ACTIVE);
    when(accountRepository.findById(1L)).thenReturn(Optional.of(customer));

    AccountStatusUpdateRequest lock = new AccountStatusUpdateRequest();
    lock.setStatus(AccountStatus.LOCKED);
    service.updateStatus(1L, lock);
    assertEquals(AccountStatus.LOCKED, customer.getStatus());

    AccountStatusUpdateRequest unlock = new AccountStatusUpdateRequest();
    unlock.setStatus(AccountStatus.ACTIVE);
    service.updateStatus(1L, unlock);
    assertEquals(AccountStatus.ACTIVE, customer.getStatus());
  }

  @Test
  void rejectsStatusChangeForNonCustomer() {
    Account admin = account(1L, Role.ROLE_ADMIN, AccountStatus.ACTIVE);
    when(accountRepository.findById(1L)).thenReturn(Optional.of(admin));
    AccountStatusUpdateRequest request = new AccountStatusUpdateRequest();
    request.setStatus(AccountStatus.LOCKED);

    assertThrows(InvalidOperationException.class, () -> service.updateStatus(1L, request));
  }

  private Account account(Long id, Role role, AccountStatus status) {
    Account account = new Account();
    account.setId(id);
    account.setEmail("account@test.com");
    account.setFullName("Test Account");
    account.setRole(role);
    account.setStatus(status);
    return account;
  }
}
