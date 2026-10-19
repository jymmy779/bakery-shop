package com.bakery.catalog.repository;

import com.bakery.catalog.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Sort;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
