package com.bakery.catalog.repository;

import com.bakery.catalog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
	java.util.Optional<Category> findByNameIgnoreCase(String name);
}
