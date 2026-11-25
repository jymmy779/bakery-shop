package com.bakery.dashboard.service.impl;

import com.bakery.account.service.AccountMetricsService;
import com.bakery.catalog.entity.ProductStatus;
import com.bakery.catalog.service.CategoryAdminService;
import com.bakery.catalog.service.ProductAdminService;
import com.bakery.dashboard.dto.CmsDashboardResponse;
import com.bakery.dashboard.service.CmsDashboardService;
import com.bakery.order.dto.OrderMetricsResponse;
import com.bakery.order.service.OrderMetricsService;
import com.bakery.order.service.RevenueQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CmsDashboardServiceImpl implements CmsDashboardService {

  private final ProductAdminService products;
  private final CategoryAdminService categories;
  private final AccountMetricsService accounts;
  private final RevenueQueryService revenue;
  private final OrderMetricsService orderMetrics;

  @Override
  @Transactional(readOnly = true)
  public CmsDashboardResponse getSummary() {
    PageRequest countOnly = PageRequest.of(0, 1);
    long totalProducts = products.search("", null, null, "newest", countOnly).getTotalElements();
    long sellingProducts =
        products.search("", null, ProductStatus.SELLING, "newest", countOnly).getTotalElements();
    long stoppedProducts =
        products.search("", null, ProductStatus.STOPPED, "newest", countOnly).getTotalElements();

    OrderMetricsResponse orders = orderMetrics.getOrderMetrics();

    return new CmsDashboardResponse(
        totalProducts,
        sellingProducts,
        stoppedProducts,
        categories.findAll().size(),
        accounts.countStaff(),
        revenue.deliveredPaidRevenue(),
        orders.pendingCount(),
        orders.confirmedCount(),
        orders.deliveringCount());
  }
}
