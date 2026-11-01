package com.bakery.account.service.impl;

import com.bakery.account.dto.AccountProfileResponse;
import com.bakery.account.dto.AccountStatusUpdateRequest;
import com.bakery.account.dto.AccountSummaryResponse;
import com.bakery.account.dto.ProfileUpdateRequest;
import com.bakery.account.dto.RegisterRequest;
import com.bakery.account.dto.StaffCreateRequest;
import com.bakery.account.entity.Account;
import com.bakery.account.entity.AccountStatus;
import com.bakery.account.entity.Role;
import com.bakery.account.repository.AccountRepository;
import com.bakery.account.service.AccountService;
import com.bakery.common.exception.ConflictException;
import com.bakery.common.exception.InvalidOperationException;
import com.bakery.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

  private final AccountRepository accountRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public AccountProfileResponse registerCustomer(RegisterRequest request) {
    String email = request.getEmail().trim().toLowerCase();

    if (accountRepository.existsByEmailIgnoreCase(email)) {
      throw new ConflictException("Email đã được sử dụng");
    }
    if (!request.getPassword().equals(request.getConfirmPassword())) {
      throw new InvalidOperationException("Mật khẩu xác nhận không khớp");
    }

    Account account = new Account();
    account.setEmail(email);
    account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    account.setFullName(request.getFullName().trim());
    account.setPhone(request.getPhone().trim());
    account.setAddress(normalizeOptional(request.getAddress()));
    account.setRole(Role.ROLE_CUSTOMER);
    account.setStatus(AccountStatus.ACTIVE);

    return toProfileResponse(accountRepository.save(account));
  }

  @Override
  public AccountSummaryResponse createStaff(StaffCreateRequest request) {
    throw new UnsupportedOperationException("Quản lý Staff chưa thuộc phạm vi triển khai hiện tại");
  }

  @Override
  @Transactional(readOnly = true)
  public AccountProfileResponse getById(Long accountId) {
    return toProfileResponse(findAccount(accountId));
  }

  @Override
  @Transactional
  public AccountProfileResponse updateProfile(Long accountId, ProfileUpdateRequest request) {
    Account account = findAccount(accountId);
    account.setFullName(request.getFullName().trim());
    account.setPhone(request.getPhone().trim());
    account.setAddress(request.getAddress().trim());
    return toProfileResponse(account);
  }

  @Override
  @Transactional
  public AccountSummaryResponse updateStatus(Long accountId, AccountStatusUpdateRequest request) {
    Account account = findAccount(accountId);
    if (account.getRole() != Role.ROLE_CUSTOMER) {
      throw new InvalidOperationException("Chỉ được khóa hoặc mở tài khoản Customer");
    }
    account.setStatus(request.getStatus());
    return toSummaryResponse(account);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<AccountSummaryResponse> findAll(Pageable pageable) {
    return accountRepository.findByRole(Role.ROLE_CUSTOMER, pageable)
        .map(this::toSummaryResponse);
  }

  private Account findAccount(Long accountId) {
    return accountRepository.findById(accountId)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
  }

  private AccountProfileResponse toProfileResponse(Account account) {
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

  private AccountSummaryResponse toSummaryResponse(Account account) {
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

  private String normalizeOptional(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
