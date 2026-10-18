package com.bakery.order.entity;

import com.bakery.account.entity.Account;
import com.bakery.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "orders",
    indexes = {
        @Index(name = "idx_orders_account", columnList = "account_id"),
        @Index(name = "idx_orders_status", columnList = "status"),
        @Index(name = "idx_orders_created_at", columnList = "created_at")
    })
public class Order extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "order_code", nullable = false, unique = true, length = 30)
  private String orderCode;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_id", nullable = false)
  private Account account;

  @Column(name = "receiver_name", nullable = false, length = 100)
  private String receiverName;

  @Column(name = "receiver_phone", nullable = false, length = 20)
  private String receiverPhone;

  @Column(name = "receiver_address", nullable = false, length = 500)
  private String receiverAddress;

  @Column(name = "receiver_email", length = 150)
  private String receiverEmail;

  @Column(name = "delivery_date", nullable = false)
  private LocalDate deliveryDate;

  @Column(name = "delivery_time")
  private LocalTime deliveryTime;

  @Column(columnDefinition = "TEXT")
  private String note;

  @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal totalAmount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private OrderStatus status;

  @Column(name = "cancellation_reason", length = 500)
  private String cancellationReason;

  @Column(name = "cancelled_at")
  private LocalDateTime cancelledAt;

  @Version
  private Long version;
}
