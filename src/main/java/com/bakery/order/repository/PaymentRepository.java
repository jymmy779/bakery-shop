package com.bakery.order.repository;

import com.bakery.order.entity.OrderStatus;
import com.bakery.order.entity.Payment;
import com.bakery.order.entity.PaymentMethod;
import com.bakery.order.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

  Optional<Payment> findByOrder_Id(Long orderId);

  @Query(value = """
      SELECT COALESCE(SUM(p.amount), 0)
      FROM payments p
      JOIN orders o ON o.id = p.order_id
      WHERE p.status = 'PAID'
        AND o.status = 'DELIVERED'
      """, nativeQuery = true)
  BigDecimal sumDeliveredPaidAmount();

  @Query(value = """
      SELECT p.id AS paymentId, o.id AS orderId, o.orderCode AS orderCode,
        o.receiverName AS customerName, p.method AS method, p.status AS paymentStatus,
        o.status AS orderStatus, p.amount AS amount, p.bankReference AS bankReference,
        p.paidAt AS paidAt, p.createdAt AS createdAt, cb.fullName AS confirmedByName
      FROM Payment p
      JOIN p.order o
      LEFT JOIN p.confirmedBy cb
      WHERE p.status = 'PAID'
        AND (:search = '' OR LOCATE(LOWER(:search), LOWER(o.orderCode)) > 0 OR LOCATE(LOWER(:search), LOWER(COALESCE(p.bankReference, ''))) > 0 OR LOCATE(LOWER(:search), LOWER(o.receiverName)) > 0)
      """, countQuery = """
      SELECT COUNT(p) FROM Payment p JOIN p.order o
      WHERE p.status = 'PAID'
        AND (:search = '' OR LOCATE(LOWER(:search), LOWER(o.orderCode)) > 0 OR LOCATE(LOWER(:search), LOWER(COALESCE(p.bankReference, ''))) > 0 OR LOCATE(LOWER(:search), LOWER(o.receiverName)) > 0)
      """)
  Page<TransactionRow> findAdminTransactions(
      @Param("search") String search,
      Pageable pageable);

  interface TransactionRow {
    Long getPaymentId();
    Long getOrderId();
    String getOrderCode();
    String getCustomerName();
    PaymentMethod getMethod();
    PaymentStatus getPaymentStatus();
    OrderStatus getOrderStatus();
    BigDecimal getAmount();
    String getBankReference();
    LocalDateTime getPaidAt();
    LocalDateTime getCreatedAt();
    String getConfirmedByName();
  }
}
