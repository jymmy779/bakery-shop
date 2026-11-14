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
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

  Optional<Order> findByIdAndAccount_Id(Long orderId, Long accountId);

  Optional<Order> findByOrderCodeAndAccount_Id(String orderCode, Long accountId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT o FROM Order o WHERE o.id = :orderId")
  Optional<Order> findForPaymentConfirmation(@Param("orderId") Long orderId);

  @Query("""
      SELECT o.id AS orderId, o.status AS orderStatus, p.status AS paymentStatus, p.paidAt AS paidAt
      FROM Order o JOIN Payment p ON p.order = o
      WHERE o.id = :orderId AND o.account.id = :accountId
      """)
  Optional<CustomerPaymentRow> findCustomerPaymentStatus(@Param("orderId") Long orderId,
      @Param("accountId") Long accountId);

  interface CustomerPaymentRow {
    Long getOrderId();
    OrderStatus getOrderStatus();
    PaymentStatus getPaymentStatus();
    LocalDateTime getPaidAt();
  }

  @Query(value = """
      SELECT o.id AS id, o.orderCode AS orderCode, o.receiverName AS customerName,
        o.totalAmount AS totalAmount, o.status AS status, p.status AS paymentStatus,
        o.deliveryDate AS deliveryDate, o.createdAt AS createdAt
      FROM Order o LEFT JOIN Payment p ON p.order = o
      WHERE (:status IS NULL OR o.status = :status)
        AND (:paymentStatus IS NULL OR p.status = :paymentStatus)
        AND (:search = '' OR LOCATE(LOWER(:search), LOWER(o.orderCode)) > 0)
      """, countQuery = """
      SELECT COUNT(o) FROM Order o LEFT JOIN Payment p ON p.order = o
      WHERE (:status IS NULL OR o.status = :status)
        AND (:paymentStatus IS NULL OR p.status = :paymentStatus)
        AND (:search = '' OR LOCATE(LOWER(:search), LOWER(o.orderCode)) > 0)
      """)
  Page<CustomerOrderRow> findAdminOrders(@Param("search") String search,
      @Param("status") OrderStatus status, @Param("paymentStatus") PaymentStatus paymentStatus,
      Pageable pageable);

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
