package com.bakery.account.service;

import com.bakery.account.dto.AccountStatusUpdateRequest;
import com.bakery.account.dto.AccountProfileResponse;
import com.bakery.account.dto.AccountSummaryResponse;
import com.bakery.account.dto.ProfileUpdateRequest;
import com.bakery.account.dto.RegisterRequest;
import com.bakery.account.dto.StaffCreateRequest;
import com.bakery.account.dto.StaffUpdateRequest;
import com.bakery.account.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AccountService {

  AccountProfileResponse registerCustomer(RegisterRequest request);

  AccountSummaryResponse createStaff(StaffCreateRequest request);

  AccountSummaryResponse createStaffAccount(StaffCreateRequest request);

  AccountProfileResponse getById(Long accountId);

  AccountProfileResponse updateProfile(Long accountId, ProfileUpdateRequest request);

  AccountSummaryResponse updateStatus(Long accountId, AccountStatusUpdateRequest request);

  AccountSummaryResponse toggleAccountStatus(Long accountId);

  AccountSummaryResponse updateStaff(Long accountId, StaffUpdateRequest request);

  Page<AccountSummaryResponse> findByRoleAndKeyword(
      Role role, String keyword, Pageable pageable);

  Page<AccountSummaryResponse> findAll(Pageable pageable);
}
