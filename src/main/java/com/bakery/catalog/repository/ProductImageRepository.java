package com.bakery.catalog.repository;

import com.bakery.catalog.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
  java.util.List<ProductImage> findByProductIdInOrderByDisplayOrderAscIdAsc(java.util.Collection<Long> productIds);
}
