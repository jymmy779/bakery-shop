package com.bakery.account.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bakery.account.dto.RegisterRequest;
import com.bakery.account.entity.Account;
import com.bakery.account.entity.Role;
import com.bakery.account.repository.AccountRepository;
import com.bakery.common.exception.ConflictException;
import com.bakery.common.exception.InvalidOperationException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

  @Mock AccountRepository accountRepository;
  @Mock PasswordEncoder passwordEncoder;

  private AccountServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new AccountServiceImpl(accountRepository, passwordEncoder);
  }

  @Test
  void registersActiveCustomerWithEncodedPassword() {
    RegisterRequest request = request("  Nguyen Van A  ", " Customer@Test.com ", "0912345678", "password123");
    when(accountRepository.existsByEmailIgnoreCase("customer@test.com")).thenReturn(false);
    when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
    when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
      Account account = invocation.getArgument(0);
      account.setId(1L);
      return account;
    });

    var response = service.registerCustomer(request);

    assertEquals(1L, response.getId());
    assertEquals("customer@test.com", response.getEmail());
    assertEquals("Nguyen Van A", response.getFullName());
    assertEquals(Role.ROLE_CUSTOMER, response.getRole());
    assertNotNull(response.getStatus());
    verify(passwordEncoder).encode("password123");
    verify(accountRepository).save(any(Account.class));
  }

  @Test
  void rejectsDuplicateEmail() {
    RegisterRequest request = request("Nguyen Van A", "customer@test.com", "0912345678", "password123");
    when(accountRepository.existsByEmailIgnoreCase("customer@test.com")).thenReturn(true);

    assertThrows(ConflictException.class, () -> service.registerCustomer(request));
  }

  @Test
  void rejectsMismatchedPasswords() {
    RegisterRequest request = request("Nguyen Van A", "customer@test.com", "0912345678", "password123");
    request.setConfirmPassword("different123");
    when(accountRepository.existsByEmailIgnoreCase("customer@test.com")).thenReturn(false);

    assertThrows(InvalidOperationException.class, () -> service.registerCustomer(request));
  }

  private RegisterRequest request(String fullName, String email, String phone, String password) {
    RegisterRequest request = new RegisterRequest();
    request.setFullName(fullName);
    request.setEmail(email);
    request.setPhone(phone);
    request.setPassword(password);
    request.setConfirmPassword(password);
    return request;
  }
}
