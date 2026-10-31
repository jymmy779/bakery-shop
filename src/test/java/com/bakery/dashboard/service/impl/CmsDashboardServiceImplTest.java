package com.bakery.dashboard.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bakery.account.service.AccountMetricsService;
import com.bakery.catalog.dto.CategoryResponse;
import com.bakery.catalog.dto.ProductSummaryResponse;
import com.bakery.catalog.entity.ProductStatus;
import com.bakery.catalog.service.CategoryAdminService;
import com.bakery.catalog.service.ProductAdminService;
import com.bakery.dashboard.dto.CmsDashboardResponse;
import com.bakery.order.service.RevenueQueryService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class CmsDashboardServiceImplTest {

  @Mock ProductAdminService products;
  @Mock CategoryAdminService categories;
  @Mock AccountMetricsService accounts;
  @Mock RevenueQueryService revenue;
  @InjectMocks CmsDashboardServiceImpl dashboard;

  @Test
  void aggregatesCurrentCmsMetricsFromFeatureServices() {
    PageRequest countOnly = PageRequest.of(0, 1);
    when(products.search("", null, null, "newest", countOnly))
        .thenReturn(pageWithTotal(12));
    when(products.search("", null, ProductStatus.SELLING, "newest", countOnly))
        .thenReturn(pageWithTotal(9));
    when(products.search("", null, ProductStatus.STOPPED, "newest", countOnly))
        .thenReturn(pageWithTotal(3));
    when(categories.findAll()).thenReturn(List.of(new CategoryResponse(), new CategoryResponse()));
    when(accounts.countUsers()).thenReturn(27L);
    when(revenue.deliveredPaidRevenue()).thenReturn(new BigDecimal("851731900.00"));

    CmsDashboardResponse result = dashboard.getSummary();

    assertEquals(new CmsDashboardResponse(12, 9, 3, 2, 27,
        new BigDecimal("851731900.00")), result);
    verify(revenue).deliveredPaidRevenue();
  }

  private PageImpl<ProductSummaryResponse> pageWithTotal(long total) {
    return new PageImpl<>(List.of(), PageRequest.of(0, 1), total);
  }

}
