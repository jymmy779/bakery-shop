package com.bakery.order.repository;

import com.bakery.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

  Optional<Order> findByIdAndAccount_Id(Long orderId, Long accountId);

  Optional<Order> findByOrderCodeAndAccount_Id(String orderCode, Long accountId);
}
