package com.bakery.account.repository;

import com.bakery.account.entity.Account;
import com.bakery.account.entity.Role;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {

  Optional<Account> findByEmailIgnoreCase(String email);

  boolean existsByEmailIgnoreCase(String email);

  Page<Account> findByRole(Role role, Pageable pageable);
}
