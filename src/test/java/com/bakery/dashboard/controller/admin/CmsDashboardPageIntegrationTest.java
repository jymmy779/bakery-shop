package com.bakery.dashboard.controller.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bakery.account.entity.Account;
import com.bakery.account.entity.AccountStatus;
import com.bakery.account.entity.Role;
import com.bakery.account.repository.AccountRepository;
import com.bakery.dashboard.dto.CmsDashboardResponse;
import com.bakery.dashboard.service.CmsDashboardService;
import com.bakery.order.service.RevenueQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
    "app.bootstrap-admin.enabled=false",
    "spring.jpa.show-sql=false",
    "logging.level.root=WARN",
    "logging.level.com.bakery=WARN"
})
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "cms-admin@example.test", roles = "ADMIN")
class CmsDashboardPageIntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired AccountRepository accountRepository;
  @Autowired RevenueQueryService revenueQueryService;
  @Autowired CmsDashboardService dashboardService;

  @BeforeEach
  void setUp() {
    Account account = accountRepository.findByEmailIgnoreCase("cms-admin@example.test")
        .orElseGet(Account::new);
    account.setEmail("cms-admin@example.test");
    account.setPasswordHash("test-password-hash");
    account.setFullName("CMS Test Admin");
    account.setRole(Role.ROLE_ADMIN);
    account.setStatus(AccountStatus.ACTIVE);
    accountRepository.save(account);
  }

  @Test
  void rendersDashboardMetricsAndNavigation() throws Exception {
    CmsDashboardResponse summary = dashboardService.getSummary();
    assertEquals(summary.totalProducts(), summary.sellingProducts() + summary.stoppedProducts());
    assertNotNull(summary.deliveredRevenue());

    mvc.perform(get("/cms"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("CMS Test Admin")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("cms-dashboard-chart-column")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("cms-dashboard-metrics-column")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("cms-dashboard-products__chart")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Biểu đồ tỷ lệ sản phẩm đang bán và ngừng bán")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString(">Tổng số sản phẩm</span>")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString(">Đang bán</span>")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString(">Ngừng bán</span>")))
        .andExpect(content().string(org.hamcrest.Matchers.not(
            org.hamcrest.Matchers.containsString("cms-dashboard-card product-selling"))))
        .andExpect(content().string(org.hamcrest.Matchers.not(
            org.hamcrest.Matchers.containsString("cms-dashboard-card product-stopped"))))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Tổng số danh mục")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Tổng số người dùng")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Doanh thu đã giao")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString(
            "Mỗi chiếc bánh nướng tại Lune là một bản giao hưởng giữa thời gian ủ men và đôi tay người thợ bánh.")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("cms-dashboard-hero__watermark")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Quản lý<br> sản phẩm")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/cms/products\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/cms/categories\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/cms/accounts\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/logout?loginType=cms\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString(
            "<main class=\"cms-page cms-dashboard-page\">")))
        .andExpect(content().string(org.hamcrest.Matchers.not(
            org.hamcrest.Matchers.containsString("<footer"))))
        .andExpect(content().string(org.hamcrest.Matchers.not(
            org.hamcrest.Matchers.containsString("<header class=\"cms-header\">"))));
  }

  @Test
  void revenueQueryExecutesAgainstPaymentAndDeliveredOrderData() {
    assertNotNull(revenueQueryService.deliveredPaidRevenue());
  }
}
