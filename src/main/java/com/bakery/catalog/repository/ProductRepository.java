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
        CASE WHEN :sort = 'price_asc' THEN p.price END ASC,
        CASE WHEN :sort = 'price_desc' THEN p.price END DESC,
        CASE WHEN :sort = 'best_selling' THEN (
          SELECT COALESCE(SUM(od.quantity), 0) FROM order_details od
          JOIN orders o ON o.id = od.order_id
          WHERE od.product_id = p.id AND o.status = 'DELIVERED'
        ) END DESC,
        p.name ASC, p.id ASC
      """, countQuery = "SELECT COUNT(*) " + FILTER_WHERE, nativeQuery = true)
  Page<Product> filter(@Param("pattern") String pattern,
      @Param("categoryId") Long categoryId, @Param("minPrice") BigDecimal minPrice,
      @Param("maxPrice") BigDecimal maxPrice, @Param("sort") String sort, Pageable pageable);
}
