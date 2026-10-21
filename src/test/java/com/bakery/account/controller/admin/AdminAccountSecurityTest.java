package com.bakery.account.controller.admin;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@SpringBootTest(properties = {
    "app.bootstrap-admin.enabled=false",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.jpa.show-sql=false",
    "logging.level.root=WARN"
})
class AdminAccountSecurityTest {

  private final MockMvc mockMvc;

  AdminAccountSecurityTest(@Autowired ApplicationContext context) {
    mockMvc = MockMvcBuilders.webAppContextSetup((org.springframework.web.context.WebApplicationContext) context)
        .apply(SecurityMockMvcConfigurers.springSecurity())
        .build();
  }

  @Test
  void customerCannotAccessStaffManagement() throws Exception {
    mockMvc.perform(get("/admin/security-probe").with(user("customer").roles("CUSTOMER")))
        .andExpect(status().isForbidden());
  }

  @Test
  void staffCanPassAdminAreaAuthorization() throws Exception {
    mockMvc.perform(get("/admin/security-probe").with(user("staff").roles("STAFF")))
        .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
  }

  @Test
  void adminCanPassAdminAreaAuthorization() throws Exception {
    mockMvc.perform(get("/admin/security-probe").with(user("admin").roles("ADMIN")))
        .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
  }
}
