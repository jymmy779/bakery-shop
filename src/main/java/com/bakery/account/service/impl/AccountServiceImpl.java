package com.bakery.account.service.impl;

import com.bakery.account.dto.AccountProfileResponse;
import com.bakery.account.dto.AccountStatusUpdateRequest;
import com.bakery.account.dto.AccountSummaryResponse;
import com.bakery.account.dto.ProfileUpdateRequest;
import com.bakery.account.dto.RegisterRequest;
import com.bakery.account.dto.StaffCreateRequest;
import com.bakery.account.dto.StaffUpdateRequest;
import com.bakery.account.entity.Account;
import com.bakery.account.entity.AccountStatus;
import com.bakery.account.entity.Role;
import com.bakery.account.repository.AccountRepository;
import com.bakery.account.service.AccountService;
import com.bakery.common.exception.ConflictException;
import com.bakery.common.exception.ResourceNotFoundException;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

  private final AccountRepository accounts;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public AccountProfileResponse registerCustomer(RegisterRequest request) {
    String email = normalizeEmail(request.getEmail());
    if (accounts.existsByEmailIgnoreCase(email)) {
      throw new ConflictException("Email này đã tồn tại trong hệ thống!");
    }
    if (!request.getPassword().equals(request.getConfirmPassword())) {
      throw new ConflictException("Mật khẩu xác nhận không khớp!");
    }
    Account account = new Account();
    account.setEmail(email);
    account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    account.setFullName(request.getFullName().trim());
    account.setPhone(request.getPhone().trim());
    account.setAddress(request.getAddress());
    account.setRole(Role.ROLE_CUSTOMER);
    account.setStatus(AccountStatus.ACTIVE);
    return toProfile(accounts.save(account));
  }

  @Override
  @Transactional
  public AccountSummaryResponse createStaff(StaffCreateRequest request) {
    return createStaffAccount(request);
  }

  @Override
  @Transactional
  public AccountSummaryResponse createStaffAccount(StaffCreateRequest request) {
    String email = normalizeEmail(request.getEmail());
    if (accounts.existsByEmailIgnoreCase(email)) {
      throw new ConflictException("Email này đã tồn tại trong hệ thống!");
    }
    Account account = new Account();
    account.setEmail(email);
    account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    account.setFullName(request.getFullName().trim());
    account.setPhone(request.getPhone().trim());
    account.setRole(Role.ROLE_STAFF);
    account.setStatus(AccountStatus.ACTIVE);
    return toSummary(accounts.save(account));
  }

  @Override
  @Transactional(readOnly = true)
  public AccountProfileResponse getById(Long accountId) {
    return toProfile(findAccount(accountId));
  }

  @Override
  @Transactional
  public AccountProfileResponse updateProfile(Long accountId, ProfileUpdateRequest request) {
    Account account = findAccount(accountId);
    account.setFullName(request.getFullName().trim());
    account.setPhone(request.getPhone().trim());
    account.setAddress(request.getAddress().trim());
    return toProfile(accounts.save(account));
  }

  @Override
  @Transactional
  public AccountSummaryResponse updateStatus(
      Long accountId, AccountStatusUpdateRequest request) {
    Account account = findAccount(accountId);
    account.setStatus(request.getStatus());
    return toSummary(accounts.save(account));
  }

  @Override
  @Transactional
  public AccountSummaryResponse toggleAccountStatus(Long accountId) {
    Account account = findAccount(accountId);
    account.setStatus(account.getStatus() == AccountStatus.ACTIVE
        ? AccountStatus.LOCKED : AccountStatus.ACTIVE);
    return toSummary(accounts.save(account));
  }

  @Override
  @Transactional
  public AccountSummaryResponse updateStaff(Long accountId, StaffUpdateRequest request) {
    Account account = findAccount(accountId);
    if (account.getRole() != Role.ROLE_STAFF) {
      throw new ConflictException("Tài khoản không phải nhân viên.");
    }
    account.setFullName(request.getFullName().trim());
    account.setPhone(request.getPhone().trim());
    return toSummary(accounts.save(account));
  }

  @Override
  @Transactional(readOnly = true)
  public Page<AccountSummaryResponse> findByRoleAndKeyword(
      Role role, String keyword, Pageable pageable) {
    String normalizedKeyword = keyword == null ? "" : keyword.trim();
    return accounts.findByRoleAndKeyword(role, normalizedKeyword, pageable).map(this::toSummary);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<AccountSummaryResponse> findAll(Pageable pageable) {
    return accounts.findAll(pageable).map(this::toSummary);
  }

  private Account findAccount(Long accountId) {
    return accounts.findById(accountId)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản."));
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  private AccountProfileResponse toProfile(Account account) {
    AccountProfileResponse response = new AccountProfileResponse();
    response.setId(account.getId());
    response.setEmail(account.getEmail());
    response.setFullName(account.getFullName());
    response.setPhone(account.getPhone());
    response.setAddress(account.getAddress());
    response.setRole(account.getRole());
    response.setStatus(account.getStatus());
    return response;
  }

  private AccountSummaryResponse toSummary(Account account) {
    AccountSummaryResponse response = new AccountSummaryResponse();
    response.setId(account.getId());
    response.setEmail(account.getEmail());
    response.setFullName(account.getFullName());
    response.setPhone(account.getPhone());
    response.setRole(account.getRole());
    response.setStatus(account.getStatus());
    response.setCreatedAt(account.getCreatedAt());
    return response;
  }
}
