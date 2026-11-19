package com.bakery.cart.repository;

import com.bakery.cart.entity.AccountCart;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountCartRepository extends JpaRepository<AccountCart, Long> {

  @Query("SELECT c FROM AccountCart c WHERE c.account.id = :accountId")
  Optional<AccountCart> findForCheckout(@Param("accountId") Long accountId);

  @EntityGraph(attributePaths = {"items", "items.product"})
  Optional<AccountCart> findByAccount_Id(Long accountId);

  @EntityGraph(attributePaths = {"items", "items.product"})
  Optional<AccountCart> findWithItemsByAccount_Id(Long accountId);
}
