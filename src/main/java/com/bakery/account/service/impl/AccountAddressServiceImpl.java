package com.bakery.account.service.impl;

import com.bakery.account.dto.AccountAddressRequest;
import com.bakery.account.dto.AccountAddressResponse;
import com.bakery.account.entity.Account;
import com.bakery.account.entity.AccountAddress;
import com.bakery.account.repository.AccountAddressRepository;
import com.bakery.account.repository.AccountRepository;
import com.bakery.account.service.AccountAddressService;
import com.bakery.common.exception.ResourceNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountAddressServiceImpl implements AccountAddressService {

  private final AccountAddressRepository addressRepository;
  private final AccountRepository accountRepository;

  @Override
  @Transactional
  public List<AccountAddressResponse> findByAccountId(Long accountId) {
    List<AccountAddress> addresses = addressRepository.findByAccountIdOrderByDefaultAddressDescIdDesc(accountId);
    if (addresses.isEmpty()) {
      Account account = findAccount(accountId);
      if (account.getAddress() != null && !account.getAddress().isBlank()) {
        AccountAddress migrated = new AccountAddress();
        migrated.setAccount(account);
        migrated.setLabel("Nhà riêng");
        migrated.setReceiverName(account.getFullName());
        migrated.setReceiverPhone(account.getPhone() == null ? "" : account.getPhone());
        migrated.setAddress(account.getAddress());
        migrated.setDefaultAddress(true);
        addresses = List.of(addressRepository.save(migrated));
      }
    }
    return addresses.stream().map(this::toResponse).toList();
  }

  @Override
  @Transactional
  public AccountAddressResponse save(Long accountId, Long addressId, AccountAddressRequest request) {
    Account account = findAccount(accountId);
    AccountAddress address = addressId == null
        ? new AccountAddress()
        : findAddress(accountId, addressId);
    boolean firstAddress = addressId == null && addressRepository.countByAccountId(accountId) == 0;

    address.setAccount(account);
    address.setLabel(request.getLabel().trim());
    address.setReceiverName(request.getReceiverName().trim());
    address.setReceiverPhone(request.getReceiverPhone().trim());
    address.setAddress(request.getAddress().trim());
    address.setDeliveryNote(normalizeOptional(request.getDeliveryNote()));
    if (firstAddress || request.isDefaultAddress()) {
      makeDefault(accountId, address);
    }
    return toResponse(addressRepository.save(address));
  }

  @Override
  @Transactional
  public void setDefault(Long accountId, Long addressId) {
    makeDefault(accountId, findAddress(accountId, addressId));
  }

  @Override
  @Transactional
  public void delete(Long accountId, Long addressId) {
    AccountAddress address = findAddress(accountId, addressId);
    boolean wasDefault = address.isDefaultAddress();
    addressRepository.delete(address);
    if (wasDefault) {
      addressRepository.findByAccountIdOrderByDefaultAddressDescIdDesc(accountId).stream()
          .findFirst().ifPresent(next -> makeDefault(accountId, next));
    }
  }

  private void makeDefault(Long accountId, AccountAddress selected) {
    addressRepository.findByAccountIdOrderByDefaultAddressDescIdDesc(accountId).forEach(address ->
        address.setDefaultAddress(address.getId().equals(selected.getId())));
    selected.setDefaultAddress(true);
  }

  private AccountAddress findAddress(Long accountId, Long addressId) {
    return addressRepository.findByIdAndAccountId(addressId, accountId)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa chỉ"));
  }

  private Account findAccount(Long accountId) {
    return accountRepository.findById(accountId)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
  }

  private AccountAddressResponse toResponse(AccountAddress address) {
    AccountAddressResponse response = new AccountAddressResponse();
    response.setId(address.getId());
    response.setLabel(address.getLabel());
    response.setReceiverName(address.getReceiverName());
    response.setReceiverPhone(address.getReceiverPhone());
    response.setAddress(address.getAddress());
    response.setDeliveryNote(address.getDeliveryNote());
    response.setDefaultAddress(address.isDefaultAddress());
    return response;
  }

  private String normalizeOptional(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
