package com.bakery.account.repository;

import com.bakery.account.entity.Account;
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

  boolean existsByEmail(String email);

  @Query("""
      SELECT a FROM Account a
      WHERE a.role = :role
        AND (:keyword = '' OR LOWER(a.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
             OR LOWER(a.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))
      ORDER BY a.createdAt DESC
      """)
  Page<Account> findByRoleAndKeyword(
      @Param("role") Role role, @Param("keyword") String keyword, Pageable pageable);
}
