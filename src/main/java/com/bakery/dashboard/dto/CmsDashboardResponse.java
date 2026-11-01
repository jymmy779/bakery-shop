package com.bakery.dashboard.dto;

import java.math.BigDecimal;

public record CmsDashboardResponse(
    long totalProducts,
    long sellingProducts,
    long stoppedProducts,
    long totalCategories,
    long totalUsers,
    BigDecimal deliveredRevenue) {
}
