package com.bakery.order.repository;

import com.bakery.order.entity.OrderStatusHistory;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

  @EntityGraph(attributePaths = "changedBy")
  List<OrderStatusHistory> findByOrder_IdOrderByChangedAtAscIdAsc(Long orderId);
}
