package com.bakery.catalog.repository;

import com.bakery.catalog.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Sort;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductRepository extends JpaRepository<Product, Long> {
  long countByCategoryId(Long categoryId);

  @Query("""
      SELECT p FROM Product p JOIN FETCH p.category c
      WHERE (:status IS NULL OR p.status = :status)
        AND (:badge IS NULL OR p.badge = :badge)
        AND (:categoryId IS NULL OR c.id = :categoryId)
        AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
      ORDER BY
        CASE WHEN :sort = 'newest' THEN p.createdAt END DESC,
        CASE WHEN :sort = 'oldest' THEN p.createdAt END ASC,
        CASE WHEN :sort = 'price_asc' THEN p.price END ASC,
        CASE WHEN :sort = 'price_desc' THEN p.price END DESC,
        CASE WHEN :sort = 'name' THEN p.name END ASC,
        p.id DESC
      """)
  Page<Product> searchAdmin(@Param("keyword") String keyword,
      @Param("categoryId") Long categoryId,
      @Param("badge") com.bakery.catalog.entity.ProductBadge badge,
      @Param("status") com.bakery.catalog.entity.ProductStatus status,
      @Param("sort") String sort,
      Pageable pageable);
  @Override
  @EntityGraph(attributePaths = "category")
  List<Product> findAll(Sort sort);

  @Query(value = """
      SELECT p.* FROM products p
      WHERE REPLACE(REPLACE(CONVERT(p.name USING utf8mb4), 'đ', 'd'), 'Đ', 'D')
                COLLATE utf8mb4_unicode_ci LIKE :pattern ESCAPE '!'
         OR REPLACE(REPLACE(CONVERT(p.description USING utf8mb4), 'đ', 'd'), 'Đ', 'D')
                COLLATE utf8mb4_unicode_ci LIKE :pattern ESCAPE '!'
      ORDER BY p.name, p.id
      """, nativeQuery = true)
  List<Product> searchByNameOrDescription(@Param("pattern") String pattern);

  String FILTER_WHERE = """
      FROM products p WHERE (:categoryId IS NULL OR p.category_id = :categoryId)
        AND (:minPrice IS NULL OR p.price >= :minPrice)
        AND (:maxPrice IS NULL OR p.price <= :maxPrice)
        AND (:pattern IS NULL OR
          REPLACE(REPLACE(CONVERT(p.name USING utf8mb4), 'đ', 'd'), 'Đ', 'D')
            COLLATE utf8mb4_unicode_ci LIKE :pattern ESCAPE '!'
          OR REPLACE(REPLACE(CONVERT(p.description USING utf8mb4), 'đ', 'd'), 'Đ', 'D')
            COLLATE utf8mb4_unicode_ci LIKE :pattern ESCAPE '!')
      """;

  @Query(value = "SELECT p.* " + FILTER_WHERE + """
      ORDER BY
        CASE WHEN :sort = 'newest' THEN p.created_at END DESC,
        CASE WHEN :sort = 'price_asc' THEN p.price END ASC,
        CASE WHEN :sort = 'price_desc' THEN p.price END DESC,
        CASE WHEN :sort = 'best_selling' THEN (
          SELECT COALESCE(SUM(od.quantity), 0) FROM order_details od
          JOIN orders o ON o.id = od.order_id
          WHERE od.product_id = p.id AND o.status = 'DELIVERED'
        ) END DESC,
        CASE WHEN :sort = 'newest' THEN p.id END DESC,
        p.name ASC, p.id ASC
      """, countQuery = "SELECT COUNT(*) " + FILTER_WHERE, nativeQuery = true)
  Page<Product> filter(@Param("pattern") String pattern,
      @Param("categoryId") Long categoryId, @Param("minPrice") BigDecimal minPrice,
      @Param("maxPrice") BigDecimal maxPrice, @Param("sort") String sort, Pageable pageable);
}
