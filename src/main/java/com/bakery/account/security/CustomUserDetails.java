package com.bakery.account.security;

import com.bakery.account.entity.Account;
import com.bakery.account.entity.AccountStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class CustomUserDetails implements UserDetails {

  private final Long accountId;
  private final String email;
  private final String passwordHash;
  private final AccountStatus status;
  private final List<GrantedAuthority> authorities;

  public CustomUserDetails(Account account) {
    this.accountId = account.getId();
    this.email = account.getEmail();
    this.passwordHash = account.getPasswordHash();
    this.status = account.getStatus();
    this.authorities = List.of(new SimpleGrantedAuthority(account.getRole().name()));
  }

  public Long getAccountId() {
    return accountId;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return passwordHash;
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public boolean isEnabled() {
    return status == AccountStatus.ACTIVE;
  }
}
