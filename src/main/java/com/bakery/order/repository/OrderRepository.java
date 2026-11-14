package com.bakery.order.repository;

import com.bakery.order.entity.Order;
import com.bakery.order.entity.OrderStatus;
import com.bakery.order.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

  Optional<Order> findByIdAndAccount_Id(Long orderId, Long accountId);

  Optional<Order> findByOrderCodeAndAccount_Id(String orderCode, Long accountId);

  @Query(value = """
      SELECT o.id AS id, o.orderCode AS orderCode, o.receiverName AS customerName,
        o.totalAmount AS totalAmount, o.status AS status, p.status AS paymentStatus,
        o.deliveryDate AS deliveryDate, o.createdAt AS createdAt
      FROM Order o LEFT JOIN Payment p ON p.order = o
      WHERE o.account.id = :accountId AND (:status IS NULL OR o.status = :status)
      """, countQuery = """
      SELECT COUNT(o) FROM Order o
      WHERE o.account.id = :accountId AND (:status IS NULL OR o.status = :status)
      """)
  Page<CustomerOrderRow> findCustomerOrders(@Param("accountId") Long accountId,
      @Param("status") OrderStatus status, Pageable pageable);

  interface CustomerOrderRow {
    Long getId();
    String getOrderCode();
    String getCustomerName();
    BigDecimal getTotalAmount();
    OrderStatus getStatus();
    PaymentStatus getPaymentStatus();
    LocalDate getDeliveryDate();
    LocalDateTime getCreatedAt();
  }
}
