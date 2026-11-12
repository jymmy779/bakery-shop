package com.bakery.account.service;

import com.bakery.account.dto.AccountAddressRequest;
import com.bakery.account.dto.AccountAddressResponse;
import java.util.List;

public interface AccountAddressService {

  List<AccountAddressResponse> findByAccountId(Long accountId);

  AccountAddressResponse save(Long accountId, Long addressId, AccountAddressRequest request);

  void setDefault(Long accountId, Long addressId);

  void delete(Long accountId, Long addressId);
}
