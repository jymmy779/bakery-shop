package com.bakery.dashboard.dto;

import java.math.BigDecimal;

public record CmsDashboardResponse(
    long totalProducts,
    long sellingProducts,
    long stoppedProducts,
    long totalCategories,
    long totalStaff,
    BigDecimal deliveredRevenue,
    long pendingOrders,
    long confirmedOrders,
    long deliveringOrders) {

  public CmsDashboardResponse(
      long totalProducts,
      long sellingProducts,
      long stoppedProducts,
      long totalCategories,
      long totalStaff,
      BigDecimal deliveredRevenue) {
    this(totalProducts, sellingProducts, stoppedProducts, totalCategories, totalStaff, deliveredRevenue, 0, 0, 0);
  }
}

