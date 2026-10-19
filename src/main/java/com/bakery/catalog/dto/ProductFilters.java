package com.bakery.catalog.dto;

import java.math.BigDecimal;

public record ProductFilters(String keyword, Long categoryId, BigDecimal minPrice,
    BigDecimal maxPrice, String sort) {
}
