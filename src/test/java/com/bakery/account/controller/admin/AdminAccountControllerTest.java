package com.bakery.account.controller.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.bakery.account.dto.AccountProfileResponse;
import com.bakery.account.dto.AccountSummaryResponse;
import com.bakery.account.dto.StaffCreateRequest;
import com.bakery.account.entity.AccountStatus;
import com.bakery.account.entity.Role;
import com.bakery.account.service.AccountService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminAccountControllerTest {

  @Mock AccountService accountService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new AdminAccountController(accountService)).build();
  }

  @Test
  void listsCustomersByCustomerRoleAndKeyword() throws Exception {
    when(accountService.findByRoleAndKeyword(eq(Role.ROLE_CUSTOMER), eq("son"), any()))
        .thenReturn(new PageImpl<>(List.of(summary(1L, Role.ROLE_CUSTOMER)),
            PageRequest.of(0, 10), 1));

    mockMvc.perform(get("/admin/customers").param("keyword", "son"))
        .andExpect(status().isOk())
        .andExpect(view().name("admin/customers"))
        .andExpect(model().attribute("keyword", "son"))
        .andExpect(model().attributeExists("customers"));

    verify(accountService).findByRoleAndKeyword(eq(Role.ROLE_CUSTOMER), eq("son"), any());
  }

  @Test
  void showsCustomerSearchErrorWhenNoResult() throws Exception {
    when(accountService.findByRoleAndKeyword(eq(Role.ROLE_CUSTOMER), eq("missing"), any()))
        .thenReturn(new PageImpl<>(List.of()));

    mockMvc.perform(get("/admin/customers").param("keyword", "missing"))
        .andExpect(model().attribute("errorMessage",
            "Không tìm thấy tài khoản khách hàng phù hợp"));
  }

  @Test
  void togglesCustomerAndAddsSuccessFlashMessage() throws Exception {
    AccountProfileResponse profile = profile(1L, Role.ROLE_CUSTOMER);
    AccountSummaryResponse locked = summary(1L, Role.ROLE_CUSTOMER);
    locked.setStatus(AccountStatus.LOCKED);
    when(accountService.getById(1L)).thenReturn(profile);
    when(accountService.toggleAccountStatus(1L)).thenReturn(locked);

    mockMvc.perform(post("/admin/customers/toggle-status/1"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/admin/customers"))
        .andExpect(flash().attribute("successMessage", "Khóa tài khoản thành công!"));
  }

  @Test
  void createsStaffAndAddsSuccessFlashMessage() throws Exception {
    when(accountService.createStaffAccount(any(StaffCreateRequest.class)))
        .thenReturn(summary(2L, Role.ROLE_STAFF));

    mockMvc.perform(post("/admin/staffs/add")
            .param("fullName", "New Staff")
            .param("email", "staff@example.com")
            .param("phone", "0901234567")
            .param("password", "secret123"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/admin/staffs"))
        .andExpect(flash().attribute("successMessage",
            "Tạo tài khoản nhân viên thành công!"));
  }

  @Test
  void returnsFieldValidationErrorsWhenCreatingInvalidStaff() throws Exception {
    mockMvc.perform(post("/admin/staffs/add")
            .param("fullName", "")
            .param("email", "not-an-email")
            .param("phone", "invalid")
            .param("password", "short"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/admin/staffs"))
        .andExpect(flash().attribute("errorMessage",
            "Vui lòng kiểm tra lại thông tin nhân viên."))
        .andExpect(flash().attribute("validationErrors",
            org.hamcrest.Matchers.hasSize(4)));
  }

  @Test
  void updatesStaffAndAddsSuccessFlashMessage() throws Exception {
    when(accountService.updateStaff(eq(2L), any()))
        .thenReturn(summary(2L, Role.ROLE_STAFF));

    mockMvc.perform(post("/admin/staffs/update/2")
            .param("fullName", "Updated Staff")
            .param("phone", "0912345678"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/admin/staffs"))
        .andExpect(flash().attribute("successMessage",
            "Cập nhật thông tin nhân viên thành công!"));
  }

  @Test
  void togglesStaffAndAddsUnlockFlashMessage() throws Exception {
    when(accountService.getById(2L)).thenReturn(profile(2L, Role.ROLE_STAFF));
    AccountSummaryResponse active = summary(2L, Role.ROLE_STAFF);
    active.setStatus(AccountStatus.ACTIVE);
    when(accountService.toggleAccountStatus(2L)).thenReturn(active);

    mockMvc.perform(post("/admin/staffs/toggle-status/2"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/admin/staffs"))
        .andExpect(flash().attribute("successMessage",
            "Mở khóa tài khoản thành công!"));
  }

  private AccountProfileResponse profile(Long id, Role role) {
    AccountProfileResponse response = new AccountProfileResponse();
    response.setId(id);
    response.setRole(role);
    response.setStatus(AccountStatus.ACTIVE);
    return response;
  }

  private AccountSummaryResponse summary(Long id, Role role) {
    AccountSummaryResponse response = new AccountSummaryResponse();
    response.setId(id);
    response.setEmail("account" + id + "@example.com");
    response.setFullName("Account " + id);
    response.setRole(role);
    response.setStatus(AccountStatus.ACTIVE);
    return response;
  }
}
