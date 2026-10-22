package com.bakery.order.repository;

import com.bakery.order.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {

  boolean existsByProductId(Long productId);
}
