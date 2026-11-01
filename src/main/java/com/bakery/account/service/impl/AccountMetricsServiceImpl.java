package com.bakery.account.service.impl;

import com.bakery.account.repository.AccountRepository;
import com.bakery.account.service.AccountMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountMetricsServiceImpl implements AccountMetricsService {

  private final AccountRepository accountRepository;

  @Override
  @Transactional(readOnly = true)
  public long countUsers() {
    return accountRepository.count();
  }
}
