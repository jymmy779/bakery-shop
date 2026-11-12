package com.bakery.account.service.impl;

import com.bakery.account.dto.AccountProfileResponse;
import com.bakery.account.dto.AccountStatusUpdateRequest;
import com.bakery.account.dto.AccountSummaryResponse;
import com.bakery.account.dto.ChangePasswordRequest;
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
import com.bakery.common.exception.InvalidOperationException;
import com.bakery.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
  @Transactional
  public AccountSummaryResponse createStaff(StaffCreateRequest request) {
    String email = request.getEmail().trim().toLowerCase();
    if (accountRepository.existsByEmailIgnoreCase(email)) {
      throw new ConflictException("Email đã được sử dụng");
    }

    Account account = new Account();
    account.setEmail(email);
    account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    account.setFullName(request.getFullName().trim());
    account.setPhone(request.getPhone().trim());
    account.setRole(Role.ROLE_STAFF);
    account.setStatus(AccountStatus.ACTIVE);
    return toSummaryResponse(accountRepository.save(account));
  }

  @Override
  @Transactional
  public AccountSummaryResponse updateStaff(Long staffId, StaffUpdateRequest request) {
    Account account = findStaff(staffId);
    String email = request.getEmail().trim().toLowerCase();
    if (accountRepository.existsByEmailIgnoreCaseAndIdNot(email, staffId)) {
      throw new ConflictException("Email đã được sử dụng");
    }

    account.setEmail(email);
    account.setFullName(request.getFullName().trim());
    account.setPhone(request.getPhone().trim());
    return toSummaryResponse(account);
  }

  @Override
  @Transactional
  public AccountSummaryResponse updateStaffStatus(Long staffId, AccountStatusUpdateRequest request) {
    Account account = findStaff(staffId);
    account.setStatus(request.getStatus());
    return toSummaryResponse(account);
  }

  @Override
  @Transactional(readOnly = true)
  public AccountSummaryResponse getStaffById(Long staffId) {
    return toSummaryResponse(findStaff(staffId));
  }

  @Override
  @Transactional(readOnly = true)
  public Page<AccountSummaryResponse> findStaff(Pageable pageable) {
    return findStaff("", null, "newest", pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<AccountSummaryResponse> findStaff(
      String search, AccountStatus status, String order, Pageable pageable) {
    return accountRepository.searchByRole(
            Role.ROLE_STAFF, normalizeSearch(search), status, withOrder(pageable, order))
        .map(this::toSummaryResponse);
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
    if (request.getPhone() != null) {
      account.setPhone(request.getPhone().trim());
    }
    if (request.getAddress() != null) {
      account.setAddress(normalizeOptional(request.getAddress()));
    }
    account.setDateOfBirth(request.getDateOfBirth());
    return toProfileResponse(account);
  }

  @Override
  @Transactional
  public void changePassword(Long accountId, ChangePasswordRequest request) {
    Account account = findAccount(accountId);
    if (!passwordEncoder.matches(request.getCurrentPassword(), account.getPasswordHash())) {
      throw new InvalidOperationException("Mật khẩu hiện tại không chính xác");
    }
    if (passwordEncoder.matches(request.getNewPassword(), account.getPasswordHash())) {
      throw new InvalidOperationException("Mật khẩu mới phải khác mật khẩu hiện tại");
    }

    account.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
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
    return findAll("", null, "newest", pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<AccountSummaryResponse> findAll(
      String search, AccountStatus status, String order, Pageable pageable) {
    return accountRepository.searchByRole(
            Role.ROLE_CUSTOMER, normalizeSearch(search), status, withOrder(pageable, order))
        .map(this::toSummaryResponse);
  }

  private Pageable withOrder(Pageable pageable, String order) {
    Sort sort = switch (order == null ? "" : order) {
      case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
      case "name_asc" -> Sort.by(Sort.Direction.ASC, "fullName");
      case "name_desc" -> Sort.by(Sort.Direction.DESC, "fullName");
      default -> Sort.by(Sort.Direction.DESC, "createdAt");
    };
    return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
  }

  private String normalizeSearch(String search) {
    return search == null ? "" : search.trim();
  }

  private Account findAccount(Long accountId) {
    return accountRepository.findById(accountId)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
  }

  private Account findStaff(Long staffId) {
    Account account = findAccount(staffId);
    if (account.getRole() != Role.ROLE_STAFF) {
      throw new InvalidOperationException("Chỉ được quản lý tài khoản Staff");
    }
    return account;
  }

  private AccountProfileResponse toProfileResponse(Account account) {
    AccountProfileResponse response = new AccountProfileResponse();
    response.setId(account.getId());
    response.setEmail(account.getEmail());
    response.setFullName(account.getFullName());
    response.setPhone(account.getPhone());
    response.setAddress(account.getAddress());
    response.setDateOfBirth(account.getDateOfBirth());
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
