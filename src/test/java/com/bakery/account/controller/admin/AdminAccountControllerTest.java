package com.bakery.account.controller.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.bakery.account.dto.AccountStatusUpdateRequest;
import com.bakery.account.dto.AccountSummaryResponse;
import com.bakery.account.dto.ChangePasswordRequest;
import com.bakery.account.entity.Account;
import com.bakery.account.entity.AccountStatus;
import com.bakery.account.entity.Role;
import com.bakery.account.security.CustomUserDetails;
import com.bakery.account.service.AccountService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith(MockitoExtension.class)
class AdminAccountControllerTest {

  @Mock AccountService accountService;

  private MockMvc mvc;
  private CustomUserDetails userDetails;

  @BeforeEach
  void setUp() {
    Account account = new Account();
    account.setId(7L);
    account.setEmail("admin@test.com");
    account.setRole(Role.ROLE_ADMIN);
    account.setStatus(AccountStatus.ACTIVE);
    userDetails = new CustomUserDetails(account);

    mvc = MockMvcBuilders.standaloneSetup(new AdminAccountController(accountService))
        .setCustomArgumentResolvers(pageableResolver(), currentUserResolver())
        .build();
  }

  @Test
  void loadsCustomerAccountsWithPageable() throws Exception {
    var pageable = PageRequest.of(0, 20);
    when(accountService.findAll(pageable)).thenReturn(new PageImpl<>(List.of(new AccountSummaryResponse()), pageable, 1));

    mvc.perform(get("/cms/accounts").param("page", "0").param("size", "20"))
        .andExpect(status().isOk())
        .andExpect(view().name("cms/account/list"))
        .andExpect(model().attributeExists("accounts"));

    verify(accountService).findAll(pageable);
  }

  @Test
  void updatesCustomerStatusAndRedirects() throws Exception {
    mvc.perform(post("/cms/accounts/7/status").param("status", "LOCKED"))
        .andExpect(status().is3xxRedirection())
        .andExpect(view().name("redirect:/cms/accounts"));

    verify(accountService).updateStatus(eq(7L), any(AccountStatusUpdateRequest.class));
  }

  @Test
  void rejectsInvalidStatusWithoutCallingService() throws Exception {
    mvc.perform(post("/cms/accounts/7/status").param("status", "UNKNOWN"))
        .andExpect(status().is3xxRedirection())
        .andExpect(view().name("redirect:/cms/accounts"));
  }

  @Test
  void loadsCmsPasswordChangePage() throws Exception {
    mvc.perform(get("/cms/profile/security"))
        .andExpect(status().isOk())
        .andExpect(view().name("cms/account/security"))
        .andExpect(model().attributeExists("changePasswordRequest"));
  }

  @Test
  void submitsCmsPasswordChangeForCurrentAccount() throws Exception {
    mvc.perform(post("/cms/profile/security")
            .param("currentPassword", "old-password")
            .param("newPassword", "new-password")
            .param("confirmPassword", "new-password"))
        .andExpect(status().is3xxRedirection())
        .andExpect(view().name("redirect:/cms/profile/security"));

    verify(accountService).changePassword(eq(7L), any(ChangePasswordRequest.class));
  }

  private HandlerMethodArgumentResolver pageableResolver() {
    return new HandlerMethodArgumentResolver() {
      @Override
      public boolean supportsParameter(MethodParameter parameter) {
        return Pageable.class.equals(parameter.getParameterType());
      }

      @Override
      public Object resolveArgument(
          MethodParameter parameter,
          ModelAndViewContainer mavContainer,
          NativeWebRequest webRequest,
          org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
        int page = Integer.parseInt(webRequest.getParameter("page"));
        int size = Integer.parseInt(webRequest.getParameter("size"));
        return PageRequest.of(page, size);
      }
    };
  }

  private HandlerMethodArgumentResolver currentUserResolver() {
    return new HandlerMethodArgumentResolver() {
      @Override
      public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(
            org.springframework.security.core.annotation.AuthenticationPrincipal.class);
      }

      @Override
      public Object resolveArgument(
          MethodParameter parameter,
          ModelAndViewContainer mavContainer,
          NativeWebRequest webRequest,
          org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
        return userDetails;
      }
    };
  }
}
