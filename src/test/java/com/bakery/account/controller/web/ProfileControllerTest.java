package com.bakery.account.controller.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.bakery.account.dto.AccountProfileResponse;
import com.bakery.account.dto.ChangePasswordRequest;
import com.bakery.account.dto.ProfileUpdateRequest;
import com.bakery.account.entity.Account;
import com.bakery.account.entity.AccountStatus;
import com.bakery.account.entity.Role;
import com.bakery.account.security.CustomUserDetails;
import com.bakery.account.service.AccountService;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

  @Mock AccountService accountService;

  private MockMvc mvc;
  private CustomUserDetails userDetails;

  @BeforeEach
  void setUp() {
    ProfileController controller = new ProfileController(accountService);
    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();
    mvc = MockMvcBuilders.standaloneSetup(controller)
        .setValidator(validator)
        .setCustomArgumentResolvers(currentUserResolver())
        .build();

    Account account = new Account();
    account.setId(7L);
    account.setEmail("customer@test.com");
    account.setPasswordHash("encoded");
    account.setRole(Role.ROLE_CUSTOMER);
    account.setStatus(AccountStatus.ACTIVE);
    userDetails = new CustomUserDetails(account);
  }

  @Test
  void loadsCurrentCustomerProfile() throws Exception {
    when(accountService.getById(7L)).thenReturn(profile());

    var result = mvc.perform(get("/profile"))
        .andExpect(status().isOk())
        .andExpect(view().name("account/profile"))
        .andExpect(model().attributeExists("profile", "profileUpdateRequest"));

    ProfileUpdateRequest request = (ProfileUpdateRequest) result.andReturn().getModelAndView()
        .getModel().get("profileUpdateRequest");
    assertEquals(LocalDate.of(2004, 8, 20), request.getDateOfBirth());

    verify(accountService).getById(7L);
  }

  @Test
  void updatesCurrentCustomerProfileWithoutAcceptingAccountId() throws Exception {
    mvc.perform(post("/profile")
            .param("fullName", "Nguyen Van B")
            .param("phone", "0987654321")
            .param("dateOfBirth", "2004-08-20"))
        .andExpect(status().is3xxRedirection())
        .andExpect(view().name("redirect:/profile?updated"));

    verify(accountService).updateProfile(eq(7L), any(ProfileUpdateRequest.class));
  }

  @Test
  void returnsProfileViewWhenUpdateValidationFails() throws Exception {
    when(accountService.getById(7L)).thenReturn(profile());

    mvc.perform(post("/profile")
            .param("fullName", "")
            .param("phone", "bad")
            .param("dateOfBirth", "2999-01-01"))
        .andExpect(status().isOk())
        .andExpect(view().name("account/profile"))
        .andExpect(model().attributeExists("profile", "profileUpdateRequest"));
  }

  @Test
  void loadsPasswordSecurityPage() throws Exception {
    mvc.perform(get("/profile/security"))
        .andExpect(status().isOk())
        .andExpect(view().name("account/security"))
        .andExpect(model().attributeExists("changePasswordRequest"));
  }

  @Test
  void changesPasswordWithValidForm() throws Exception {
    mvc.perform(post("/profile/security")
            .param("currentPassword", "old-password")
            .param("newPassword", "new-password")
            .param("confirmPassword", "new-password"))
        .andExpect(status().is3xxRedirection())
        .andExpect(view().name("redirect:/profile/security?updated"));

    verify(accountService).changePassword(eq(7L), any(ChangePasswordRequest.class));
  }

  @Test
  void showsValidationErrorWhenNewPasswordsDoNotMatch() throws Exception {
    mvc.perform(post("/profile/security")
            .param("currentPassword", "old-password")
            .param("newPassword", "new-password")
            .param("confirmPassword", "different-password"))
        .andExpect(status().isOk())
        .andExpect(view().name("account/security"))
        .andExpect(model().attributeExists("changePasswordRequest"));
  }

  private AccountProfileResponse profile() {
    AccountProfileResponse profile = new AccountProfileResponse();
    profile.setId(7L);
    profile.setEmail("customer@test.com");
    profile.setFullName("Nguyen Van A");
    profile.setPhone("0912345678");
    profile.setAddress("Dia chi cu");
    profile.setDateOfBirth(LocalDate.of(2004, 8, 20));
    profile.setRole(Role.ROLE_CUSTOMER);
    profile.setStatus(AccountStatus.ACTIVE);
    return profile;
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
