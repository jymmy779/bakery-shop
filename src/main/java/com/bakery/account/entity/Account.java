package com.bakery.account.entity;

import com.bakery.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "accounts",
    indexes = {
        @Index(name = "idx_accounts_role", columnList = "role"),
        @Index(name = "idx_accounts_status", columnList = "status")
    })
public class Account extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 150)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "full_name", nullable = false, length = 100)
  private String fullName;

  @Column(length = 20)
  private String phone;

  @Column(length = 500)
  private String address;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private Role role;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private AccountStatus status;

  @Version
  private Long version;
}
