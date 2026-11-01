package com.bakery.order.repository;

import com.bakery.order.entity.Payment;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

  @Query(value = """
      SELECT COALESCE(SUM(p.amount), 0)
      FROM payments p
      JOIN orders o ON o.id = p.order_id
      WHERE p.status = 'PAID'
        AND o.status = 'DELIVERED'
      """, nativeQuery = true)
  BigDecimal sumDeliveredPaidAmount();
}
