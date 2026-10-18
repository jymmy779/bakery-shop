package com.bakery.account.security;

import com.bakery.account.entity.Account;
import com.bakery.account.repository.AccountRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

  private final AccountRepository accountRepository;

  public CustomUserDetailsService(AccountRepository accountRepository) {
    this.accountRepository = accountRepository;
  }

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    Account account = accountRepository.findByEmailIgnoreCase(email.trim())
        .orElseThrow(() -> new UsernameNotFoundException("Account not found"));

    return new CustomUserDetails(account);
  }
}
