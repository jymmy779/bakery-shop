package com.bakery.order.repository;

import com.bakery.account.entity.Account;
import com.bakery.catalog.entity.Product;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** Associates validated service IDs with order foreign keys without querying other repositories. */
@Repository
@RequiredArgsConstructor
public class CheckoutReferenceRepository {

  private final EntityManager entityManager;

  public Account account(Long accountId) {
    return entityManager.getReference(Account.class, accountId);
  }

  public Product product(Long productId) {
    return entityManager.getReference(Product.class, productId);
  }
}
