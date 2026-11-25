package com.bakery.account.repository;

import com.bakery.account.entity.Account;
import com.bakery.account.entity.AccountStatus;
import com.bakery.account.entity.Role;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, Long> {

  Optional<Account> findByEmailIgnoreCase(String email);

  boolean existsByEmailIgnoreCase(String email);

  boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

  Page<Account> findByRole(Role role, Pageable pageable);

  long countByRole(Role role);

  @Query("""
      select account from Account account
      where account.role = :role
        and (:status is null or account.status = :status)
        and (:search = ''
          or lower(account.fullName) like lower(concat('%', :search, '%'))
          or lower(account.email) like lower(concat('%', :search, '%'))
          or lower(coalesce(account.phone, '')) like lower(concat('%', :search, '%')))
      """)
  Page<Account> searchByRole(
      @Param("role") Role role,
      @Param("search") String search,
      @Param("status") AccountStatus status,
      Pageable pageable);
}
