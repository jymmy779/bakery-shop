package com.bakery.catalog.entity;

import com.bakery.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "product_images",
    indexes = @Index(name = "idx_product_images_product", columnList = "product_id"),
    uniqueConstraints = @UniqueConstraint(
        name = "uk_product_images_product_order",
        columnNames = {"product_id", "display_order"}))
public class ProductImage extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Column(name = "image_path", nullable = false, length = 500)
  private String imagePath;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;
}
