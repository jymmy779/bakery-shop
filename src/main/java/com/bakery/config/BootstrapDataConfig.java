package com.bakery.config;

import com.bakery.account.entity.Account;
import com.bakery.account.entity.AccountStatus;
import com.bakery.account.entity.Role;
import com.bakery.account.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class BootstrapDataConfig implements ApplicationRunner {

  private final AccountRepository accountRepository;
  private final PasswordEncoder passwordEncoder;
  private final boolean enabled;
  private final String adminEmail;
  private final String adminPassword;
  private final String adminName;

  public BootstrapDataConfig(
      AccountRepository accountRepository,
      PasswordEncoder passwordEncoder,
      @Value("${app.bootstrap-admin.enabled:true}") boolean enabled,
      @Value("${app.bootstrap-admin.email:admin@bakery.local}") String adminEmail,
      @Value("${app.bootstrap-admin.password:admin123}") String adminPassword,
      @Value("${app.bootstrap-admin.name:Bakery Admin}") String adminName) {
    this.accountRepository = accountRepository;
    this.passwordEncoder = passwordEncoder;
    this.enabled = enabled;
    this.adminEmail = adminEmail;
    this.adminPassword = adminPassword;
    this.adminName = adminName;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!enabled || accountRepository.existsByEmailIgnoreCase(adminEmail)) {
      return;
    }

    Account admin = new Account();
    admin.setEmail(adminEmail.trim().toLowerCase());
    admin.setPasswordHash(passwordEncoder.encode(adminPassword));
    admin.setFullName(adminName);
    admin.setRole(Role.ROLE_ADMIN);
    admin.setStatus(AccountStatus.ACTIVE);
    accountRepository.save(admin);
  }
}
