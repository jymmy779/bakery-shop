package com.bakery.catalog.dto;

import java.util.List;

public record ProductListResponse(String message, List<ProductSummaryResponse> products) {
}
