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
    account(adminEmail, "admin-password", Role.ROLE_ADMIN, AccountStatus.ACTIVE);
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
  void customerCanLoginAndAccessProfile() throws Exception {
    MockHttpSession session = (MockHttpSession) mvc.perform(post("/login").with(csrf())
            .param("email", customerEmail)
            .param("password", "customer-password"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/"))
        .andReturn().getRequest().getSession();

    mvc.perform(get("/profile").session(session))
        .andExpect(status().isOk());
  }

  @Test
  void adminCanViewAccountsButCustomerAndStaffCannot() throws Exception {
    var adminSession = login(adminEmail, "admin-password", "cms");
    var customerSession = login(customerEmail, "customer-password", null);
    var staffSession = login(staffEmail, "staff-password", "cms");

    mvc.perform(get("/cms/accounts").session(adminSession))
        .andExpect(status().isOk());
    mvc.perform(get("/cms/accounts").session(customerSession))
        .andExpect(status().isForbidden());
    mvc.perform(get("/cms/accounts").session(staffSession))
        .andExpect(status().isForbidden());
  }

  @Test
  void lockedCustomerCannotLogin() throws Exception {
    mvc.perform(post("/login").with(csrf())
            .param("email", lockedCustomerEmail)
            .param("password", "locked-password"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login?error"));
  }

  @Test
  void logoutInvalidatesAuthenticatedSession() throws Exception {
    var session = login(customerEmail, "customer-password", null);

    mvc.perform(post("/logout").session(session).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login?logout"));

    mvc.perform(get("/profile").session(session))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));
  }

  private MockHttpSession login(String email, String password, String loginType)
      throws Exception {
    var request = post("/login").with(csrf())
        .param("email", email)
        .param("password", password);
    if (loginType != null) {
      request = request.param("loginType", loginType);
    }
    return (MockHttpSession) mvc.perform(request)
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
