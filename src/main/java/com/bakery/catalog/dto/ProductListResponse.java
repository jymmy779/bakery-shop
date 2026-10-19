package com.bakery.catalog.dto;

import java.util.List;

public record ProductListResponse(String message, List<ProductSummaryResponse> products,
    int page, int size, long totalElements, int totalPages, ProductFilters filters,
    String next, String previous) {
}
