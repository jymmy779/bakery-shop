package com.bakery.cart.repository;

import com.bakery.cart.entity.AccountCart;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;

public interface AccountCartRepository extends JpaRepository<AccountCart, Long> {

  @EntityGraph(attributePaths = {"items", "items.product"})
  Optional<AccountCart> findByAccount_Id(Long accountId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @EntityGraph(attributePaths = {"items", "items.product"})
  Optional<AccountCart> findWithItemsByAccount_Id(Long accountId);
}
