package com.bakery.account.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bakery.account.entity.Account;
import com.bakery.account.entity.AccountStatus;
import com.bakery.account.entity.Role;
import com.bakery.account.repository.AccountRepository;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.web.MockHttpSession;
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
class AccountSecurityIntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired AccountRepository accountRepository;
  @Autowired PasswordEncoder passwordEncoder;
  @Autowired EntityManager entityManager;

  private String marker;
  private String customerEmail;
  private String adminEmail;
  private String staffEmail;
  private String lockedCustomerEmail;

  @BeforeEach
  void setUp() {
    marker = UUID.randomUUID().toString().replace("-", "");
    customerEmail = "customer-" + marker + "@example.test";
    adminEmail = "admin-" + marker + "@example.test";
    staffEmail = "staff-" + marker + "@example.test";
    lockedCustomerEmail = "locked-" + marker + "@example.test";

    account(customerEmail, "customer-password", Role.ROLE_CUSTOMER, AccountStatus.ACTIVE);
    Account admin = account(adminEmail, "admin-password", Role.ROLE_ADMIN, AccountStatus.ACTIVE);
    admin.setAddress("Office address");
    account(staffEmail, "staff-password", Role.ROLE_STAFF, AccountStatus.ACTIVE);
    account(lockedCustomerEmail, "locked-password", Role.ROLE_CUSTOMER, AccountStatus.LOCKED);
    entityManager.flush();
  }

  @Test
  void registrationRequiresCsrfAndCreatesCustomer() throws Exception {
    String email = "new-" + marker + "@example.test";

    mvc.perform(post("/register")
            .param("fullName", "New Customer")
            .param("email", email)
            .param("phone", "0912345678")
            .param("address", "Test address")
            .param("password", "customer123")
            .param("confirmPassword", "customer123"))
        .andExpect(status().isForbidden());

    mvc.perform(post("/register").with(csrf())
            .param("fullName", "New Customer")
            .param("email", email)
            .param("phone", "0912345678")
            .param("address", "Test address")
            .param("password", "customer123")
            .param("confirmPassword", "customer123"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login?registered"));

    Account account = accountRepository.findByEmailIgnoreCase(email).orElseThrow();
    assertEquals(Role.ROLE_CUSTOMER, account.getRole());
    assertEquals(AccountStatus.ACTIVE, account.getStatus());
    assertTrue(passwordEncoder.matches("customer123", account.getPasswordHash()));
  }

  @Test
  void customerLoginDefaultsToHomeAndCanOpenProfile() throws Exception {
    MockHttpSession session = (MockHttpSession) mvc.perform(post("/login").with(csrf())
            .param("email", customerEmail)
            .param("password", "customer-password")
            .param("loginType", "customer"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/"))
        .andReturn().getRequest().getSession();

    mvc.perform(get("/profile").session(session))
        .andExpect(status().isOk());
  }

  @Test
  void customerReturnsToSavedPageAfterLogin() throws Exception {
    MockHttpSession session = (MockHttpSession) mvc.perform(
            get("/orders/42?tab=details"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"))
        .andReturn().getRequest().getSession();

    mvc.perform(post("/login").session(session).with(csrf())
            .param("email", customerEmail)
            .param("password", "customer-password")
            .param("loginType", "customer"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/orders/42?tab=details"));
  }

  @Test
  void adminAlwaysReturnsToCmsDashboardAfterLogin() throws Exception {
    MockHttpSession session = (MockHttpSession) mvc.perform(get("/cms/accounts?page=1&size=10"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/cms/login"))
        .andReturn().getRequest().getSession();

    mvc.perform(post("/login").session(session).with(csrf())
            .param("email", adminEmail)
            .param("password", "admin-password")
            .param("loginType", "cms"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/cms"));
  }

  @Test
  void staffCanUseCmsLoginAndIsRedirectedToCms() throws Exception {
    mvc.perform(post("/login").with(csrf())
            .param("email", staffEmail)
            .param("password", "staff-password")
            .param("loginType", "cms"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/cms"));
  }

  @Test
  void customerCannotUseCmsLoginPage() throws Exception {
    mvc.perform(post("/login").with(csrf())
            .param("email", customerEmail)
            .param("password", "customer-password")
            .param("loginType", "cms"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/cms/login?roleError"));
  }

  @Test
  void staffCannotUseCustomerLoginPage() throws Exception {
    mvc.perform(post("/login").with(csrf())
            .param("email", staffEmail)
            .param("password", "staff-password")
            .param("loginType", "customer"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login?roleError"));
  }

  @Test
  void cmsLoginRouteRendersSeparateLoginPage() throws Exception {
    mvc.perform(get("/cms/login"))
        .andExpect(status().isOk())
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.view()
            .name("cms/login"));
  }

  @Test
  void customerDoesNotReturnToSavedCmsPage() throws Exception {
    MockHttpSession session = (MockHttpSession) mvc.perform(get("/cms/accounts"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/cms/login"))
        .andReturn().getRequest().getSession();

    mvc.perform(post("/login").session(session).with(csrf())
            .param("email", customerEmail)
            .param("password", "customer-password")
            .param("loginType", "customer"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/"));
  }

  @Test
  void adminCanViewAccountsButCustomerAndStaffCannot() throws Exception {
    var adminSession = login(adminEmail, "admin-password", "cms");
    var customerSession = login(customerEmail, "customer-password", "customer");
    var staffSession = login(staffEmail, "staff-password", "cms");

    mvc.perform(get("/cms/accounts").session(adminSession))
        .andExpect(status().isOk());
    mvc.perform(get("/cms/accounts").session(customerSession))
        .andExpect(status().isForbidden());
    mvc.perform(get("/cms/accounts").session(staffSession))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminCanViewAndUpdateOwnCmsProfile() throws Exception {
    var adminSession = login(adminEmail, "admin-password", "cms");

    mvc.perform(get("/cms/profile").session(adminSession))
        .andExpect(status().isOk())
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.view()
            .name("cms/account/profile"));

    mvc.perform(post("/cms/profile").session(adminSession).with(csrf())
            .param("fullName", "Updated Admin")
            .param("phone", "0912345678"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/cms/profile"));

    Account updated = accountRepository.findByEmailIgnoreCase(adminEmail).orElseThrow();
    assertEquals("Updated Admin", updated.getFullName());
    assertEquals("Office address", updated.getAddress());
  }

  @Test
  void lockedCustomerCannotLogin() throws Exception {
    mvc.perform(post("/login").with(csrf())
            .param("email", lockedCustomerEmail)
            .param("password", "locked-password")
            .param("loginType", "customer"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login?error"));
  }

  @Test
  void logoutInvalidatesAuthenticatedSession() throws Exception {
    var session = login(customerEmail, "customer-password", "customer");

    mvc.perform(post("/logout").session(session).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login?logout"));

    mvc.perform(get("/profile").session(session))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));
  }

  @Test
  void cmsLogoutReturnsToCmsLoginAndInvalidatesSession() throws Exception {
    var session = login(staffEmail, "staff-password", "cms");

    mvc.perform(post("/logout").session(session).with(csrf()).param("loginType", "cms"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/cms/login?logout"));

    mvc.perform(get("/cms").session(session))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/cms/login"));
  }

  private MockHttpSession login(String email, String password, String loginType) throws Exception {
    return (MockHttpSession) mvc.perform(post("/login").with(csrf())
        .param("email", email)
        .param("password", password)
        .param("loginType", loginType))
        .andExpect(status().is3xxRedirection())
        .andReturn().getRequest().getSession();
  }

  private Account account(String email, String password, Role role, AccountStatus status) {
    Account account = new Account();
    account.setEmail(email);
    account.setPasswordHash(passwordEncoder.encode(password));
    account.setFullName("Integration Test Account");
    account.setRole(role);
    account.setStatus(status);
    entityManager.persist(account);
    return account;
  }
}
