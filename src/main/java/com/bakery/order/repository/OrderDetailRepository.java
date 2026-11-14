package com.bakery.order.repository;

import com.bakery.order.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {

  boolean existsByProductId(Long productId);

  List<OrderDetail> findByOrder_IdOrderByIdAsc(Long orderId);
}
