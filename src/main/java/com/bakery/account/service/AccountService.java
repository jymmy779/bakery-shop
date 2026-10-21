package com.bakery.account.service;

import com.bakery.account.dto.AccountStatusUpdateRequest;
import com.bakery.account.dto.AccountProfileResponse;
import com.bakery.account.dto.AccountSummaryResponse;
import com.bakery.account.dto.ProfileUpdateRequest;
import com.bakery.account.dto.RegisterRequest;
import com.bakery.account.dto.StaffCreateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AccountService {

  AccountProfileResponse registerCustomer(RegisterRequest request);

  AccountSummaryResponse createStaff(StaffCreateRequest request);

  AccountProfileResponse getById(Long accountId);

  AccountProfileResponse updateProfile(Long accountId, ProfileUpdateRequest request);

  AccountSummaryResponse updateStatus(Long accountId, AccountStatusUpdateRequest request);

  Page<AccountSummaryResponse> findAll(Pageable pageable);
}
