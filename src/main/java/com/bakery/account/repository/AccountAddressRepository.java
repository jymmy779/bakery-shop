package com.bakery.account.repository;

import com.bakery.account.entity.AccountAddress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountAddressRepository extends JpaRepository<AccountAddress, Long> {

  List<AccountAddress> findByAccountIdOrderByDefaultAddressDescIdDesc(Long accountId);

  Optional<AccountAddress> findByIdAndAccountId(Long id, Long accountId);

  long countByAccountId(Long accountId);
}
