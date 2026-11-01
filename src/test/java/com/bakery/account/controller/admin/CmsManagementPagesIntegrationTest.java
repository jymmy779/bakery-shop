package com.bakery.account.controller.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bakery.account.dto.AccountSummaryResponse;
import com.bakery.account.entity.Account;
import com.bakery.account.entity.AccountStatus;
import com.bakery.account.entity.Role;
import com.bakery.account.repository.AccountRepository;
import com.bakery.account.service.AccountService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
class CmsManagementPagesIntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired AccountRepository accountRepository;
  @MockitoBean AccountService accountService;

  @BeforeEach
  void setUp() {
    Account admin = accountRepository.findByEmailIgnoreCase("cms-admin@example.test")
        .orElseGet(Account::new);
    admin.setEmail("cms-admin@example.test");
    admin.setPasswordHash("test-password-hash");
    admin.setFullName("CMS Test Admin");
    admin.setRole(Role.ROLE_ADMIN);
    admin.setStatus(AccountStatus.ACTIVE);
    accountRepository.save(admin);

    AccountSummaryResponse customer = new AccountSummaryResponse();
    customer.setId(17L);
    customer.setFullName("Bakery Customer");
    customer.setEmail("customer@example.test");
    customer.setPhone("0900000000");
    customer.setRole(Role.ROLE_CUSTOMER);
    customer.setStatus(AccountStatus.ACTIVE);
    customer.setCreatedAt(LocalDateTime.of(2026, 10, 1, 10, 0));
    when(accountService.findAll(any())).thenReturn(
        new PageImpl<>(List.of(customer), PageRequest.of(0, 20), 1));
  }

  @Test
  void rendersAccountManagementWithSharedCmsShell() throws Exception {
    mvc.perform(get("/cms/accounts"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("CMS Test Admin")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Bakery Customer")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("customer@example.test")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/cms/accounts/17/status\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"_csrf\"")))
        .andExpect(content().string(org.hamcrest.Matchers.not(
            org.hamcrest.Matchers.containsString("<header class=\"cms-header\">"))))
        .andExpect(content().string(org.hamcrest.Matchers.not(
            org.hamcrest.Matchers.containsString("<footer"))));
  }

  @Test
  void rendersStaffManagementPlaceholderWithSharedCmsShell() throws Exception {
    mvc.perform(get("/cms/staff"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Quản lý")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Chức năng đang được hoàn thiện")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("cms-sidebar-nav")))
        .andExpect(content().string(org.hamcrest.Matchers.not(
            org.hamcrest.Matchers.containsString("<footer"))));
  }

  @Test
  void rendersOrderManagementPlaceholderWithSharedCmsShell() throws Exception {
    mvc.perform(get("/cms/orders"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Danh sách đơn hàng")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString(
            "Danh sách đơn hàng đang được tích hợp")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("cms-sidebar-nav")))
        .andExpect(content().string(org.hamcrest.Matchers.not(
            org.hamcrest.Matchers.containsString("<footer"))));
  }
}
