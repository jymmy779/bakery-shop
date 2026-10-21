package com.bakery.account.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bakery.account.dto.RegisterRequest;
import com.bakery.account.dto.StaffCreateRequest;
import com.bakery.account.dto.StaffUpdateRequest;
import com.bakery.account.entity.Account;
import com.bakery.account.entity.AccountStatus;
import com.bakery.account.entity.Role;
import com.bakery.account.repository.AccountRepository;
import com.bakery.common.exception.ConflictException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

  @Mock AccountRepository accounts;
  @Mock PasswordEncoder passwordEncoder;

  private AccountServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new AccountServiceImpl(accounts, passwordEncoder);
  }

  @Test
  void createsStaffWithEncodedPasswordAndStaffRole() {
    StaffCreateRequest request = staffRequest("Staff", " STAFF@EXAMPLE.COM ", "secret123");
    when(accounts.existsByEmailIgnoreCase("staff@example.com")).thenReturn(false);
    when(passwordEncoder.encode("secret123")).thenReturn("bcrypt-hash");
    when(accounts.save(any(Account.class))).thenAnswer(invocation -> {
      Account account = invocation.getArgument(0);
      account.setId(10L);
      return account;
    });

    var response = service.createStaffAccount(request);

    ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
    verify(accounts).save(captor.capture());
    Account saved = captor.getValue();
    assertEquals("staff@example.com", saved.getEmail());
    assertEquals("bcrypt-hash", saved.getPasswordHash());
    assertEquals(Role.ROLE_STAFF, saved.getRole());
    assertEquals(AccountStatus.ACTIVE, saved.getStatus());
    assertNotEquals("secret123", saved.getPasswordHash());
    assertEquals(10L, response.getId());
  }

  @Test
  void rejectsStaffWithDuplicateEmail() {
    StaffCreateRequest request = staffRequest("Staff", "staff@example.com", "secret123");
    when(accounts.existsByEmailIgnoreCase("staff@example.com")).thenReturn(true);

    assertThrows(ConflictException.class, () -> service.createStaffAccount(request));
  }

  @Test
  void registersCustomerWithEncodedPasswordAndCustomerRole() {
    RegisterRequest request = new RegisterRequest();
    request.setFullName(" Customer ");
    request.setEmail(" CUSTOMER@EXAMPLE.COM ");
    request.setPhone("0901234567");
    request.setAddress("Address");
    request.setPassword("secret123");
    request.setConfirmPassword("secret123");
    when(accounts.existsByEmailIgnoreCase("customer@example.com")).thenReturn(false);
    when(passwordEncoder.encode("secret123")).thenReturn("customer-hash");
    when(accounts.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

    var response = service.registerCustomer(request);

    ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
    verify(accounts).save(captor.capture());
    assertEquals(Role.ROLE_CUSTOMER, captor.getValue().getRole());
    assertEquals(AccountStatus.ACTIVE, captor.getValue().getStatus());
    assertEquals("customer-hash", captor.getValue().getPasswordHash());
    assertEquals("customer@example.com", response.getEmail());
  }

  @Test
  void togglesAccountBetweenActiveAndLocked() {
    Account account = account(20L, Role.ROLE_CUSTOMER, AccountStatus.ACTIVE);
    when(accounts.findById(20L)).thenReturn(Optional.of(account));
    when(accounts.save(account)).thenReturn(account);

    service.toggleAccountStatus(20L);
    assertEquals(AccountStatus.LOCKED, account.getStatus());

    service.toggleAccountStatus(20L);
    assertEquals(AccountStatus.ACTIVE, account.getStatus());
  }

  @Test
  void updatesStaffNameAndPhoneOnlyForStaff() {
    Account account = account(21L, Role.ROLE_STAFF, AccountStatus.ACTIVE);
    when(accounts.findById(21L)).thenReturn(Optional.of(account));
    when(accounts.save(account)).thenReturn(account);
    StaffUpdateRequest request = new StaffUpdateRequest();
    request.setFullName(" Updated Staff ");
    request.setPhone("0912345678");

    service.updateStaff(21L, request);

    assertEquals("Updated Staff", account.getFullName());
    assertEquals("0912345678", account.getPhone());
    verify(accounts).save(account);
  }

  @Test
  void rejectsUpdatingCustomerAsStaff() {
    when(accounts.findById(22L)).thenReturn(
        Optional.of(account(22L, Role.ROLE_CUSTOMER, AccountStatus.ACTIVE)));

    assertThrows(ConflictException.class, () -> service.updateStaff(22L, new StaffUpdateRequest()));
  }

  @Test
  void searchesAccountsByRoleAndTrimmedKeyword() {
    PageRequest pageable = PageRequest.of(0, 10);
    when(accounts.findByRoleAndKeyword(Role.ROLE_CUSTOMER, "son", pageable))
        .thenReturn(new PageImpl<>(java.util.List.of(
            account(30L, Role.ROLE_CUSTOMER, AccountStatus.ACTIVE)), pageable, 1));

    var page = service.findByRoleAndKeyword(Role.ROLE_CUSTOMER, " son ", pageable);

    assertEquals(1, page.getTotalElements());
    assertEquals(Role.ROLE_CUSTOMER, page.getContent().get(0).getRole());
    verify(accounts).findByRoleAndKeyword(Role.ROLE_CUSTOMER, "son", pageable);
  }

  private StaffCreateRequest staffRequest(String fullName, String email, String password) {
    StaffCreateRequest request = new StaffCreateRequest();
    request.setFullName(fullName);
    request.setEmail(email);
    request.setPhone("0901234567");
    request.setPassword(password);
    return request;
  }

  private Account account(Long id, Role role, AccountStatus status) {
    Account account = new Account();
    account.setId(id);
    account.setEmail("account" + id + "@example.com");
    account.setFullName("Account " + id);
    account.setPhone("0901234567");
    account.setRole(role);
    account.setStatus(status);
    return account;
  }
}
