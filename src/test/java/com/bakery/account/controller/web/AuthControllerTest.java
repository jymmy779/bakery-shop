package com.bakery.account.controller.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.bakery.account.dto.RegisterRequest;
import com.bakery.account.service.AccountService;
import com.bakery.common.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  @Mock AccountService accountService;

  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    AuthController controller = new AuthController(accountService);
    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();
    mvc = MockMvcBuilders.standaloneSetup(controller).setValidator(validator).build();
  }

  @Test
  void acceptsValidRegistrationAndRedirectsToLogin() throws Exception {
    mvc.perform(post("/register")
            .param("fullName", "Nguyen Van A")
            .param("email", "customer@test.com")
            .param("phone", "0912345678")
            .param("address", "Ho Chi Minh")
            .param("password", "password123")
            .param("confirmPassword", "password123"))
        .andExpect(status().is3xxRedirection())
        .andExpect(view().name("redirect:/login?registered"));

    verify(accountService).registerCustomer(any(RegisterRequest.class));
  }

  @Test
  void returnsRegistrationViewWhenValidationFails() throws Exception {
    mvc.perform(post("/register")
            .param("fullName", "")
            .param("email", "not-an-email")
            .param("phone", "bad")
            .param("password", "short")
            .param("confirmPassword", "short"))
        .andExpect(status().isOk())
        .andExpect(view().name("auth/register"))
        .andExpect(model().attributeExists("registerRequest"));
  }

  @Test
  void returnsRegistrationViewWhenEmailAlreadyExists() throws Exception {
    doThrow(new ConflictException("Email đã được sử dụng"))
        .when(accountService).registerCustomer(any(RegisterRequest.class));

    mvc.perform(post("/register")
            .param("fullName", "Nguyen Van A")
            .param("email", "customer@test.com")
            .param("phone", "0912345678")
            .param("password", "password123")
            .param("confirmPassword", "password123"))
        .andExpect(status().isOk())
        .andExpect(view().name("auth/register"))
        .andExpect(model().attributeHasErrors("registerRequest"));
  }
}
