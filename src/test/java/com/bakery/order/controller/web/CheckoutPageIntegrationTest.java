package com.bakery.order.controller.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import com.bakery.account.dto.AccountAddressResponse;
import com.bakery.account.dto.AccountProfileResponse;
import com.bakery.account.entity.Account;
import com.bakery.account.entity.AccountStatus;
import com.bakery.account.entity.Role;
import com.bakery.account.security.CustomUserDetails;
import com.bakery.account.service.AccountAddressService;
import com.bakery.account.service.AccountService;
import com.bakery.cart.dto.CartItemResponse;
import com.bakery.cart.dto.CartResponse;
import com.bakery.cart.service.impl.SessionCartServiceImpl;
import com.bakery.config.SecurityConfig;
import com.bakery.order.service.impl.CheckoutPageServiceImpl;
import com.bakery.order.service.impl.CheckoutQuoteStore;
import com.bakery.order.service.impl.BankTransferProperties;
import com.bakery.order.service.impl.BankTransferServiceImpl;
import com.bakery.order.service.impl.CheckoutSubmissionServiceImpl;
import com.bakery.order.service.impl.CheckoutResultServiceImpl;
import com.bakery.order.dto.OrderDetailResponse;
import com.bakery.common.exception.ConflictException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(CheckoutController.class)
@Import({SecurityConfig.class, CheckoutPageServiceImpl.class, CheckoutQuoteStore.class,
    BankTransferServiceImpl.class, BankTransferProperties.class})
class CheckoutPageIntegrationTest {

  @Autowired MockMvc mvc;
  @MockitoBean SessionCartServiceImpl carts;
  @MockitoBean AccountService accounts;
  @MockitoBean AccountAddressService addresses;
  @MockitoBean CheckoutSubmissionServiceImpl submissions;
  @MockitoBean CheckoutResultServiceImpl results;
  @MockitoBean com.bakery.account.repository.AccountRepository accountRepository;

  private final MockHttpSession session = new MockHttpSession();
  private CartResponse cart;
  private CustomUserDetails customer;

  @BeforeEach
  void setup() {
    var account = new Account();
    account.setId(7L);
    account.setEmail("hung@example.com");
    account.setPasswordHash("unused");
    account.setRole(Role.ROLE_CUSTOMER);
    account.setStatus(AccountStatus.ACTIVE);
    customer = new CustomUserDetails(account);

    var profile = new AccountProfileResponse();
    profile.setFullName("Phạm Nhật Hưng");
    profile.setPhone("0912345678");
    profile.setAddress("12 Nguyễn Huệ, Quận 1, TP. Hồ Chí Minh");
    profile.setEmail("hung@example.com");
    when(accounts.getById(7L)).thenReturn(profile);
    when(addresses.findByAccountId(7L)).thenReturn(List.of());

    var cake = new CartItemResponse();
    cake.setProductId(42L);
    cake.setProductName("Bánh kem dâu <đặc biệt>");
    cake.setUnitPrice(new BigDecimal("250000"));
    cake.setQuantity(2);
    cake.setLineTotal(new BigDecimal("500000"));
    cake.setAvailableStock(5);
    cake.setPreparationDays(2);
    cart = new CartResponse();
    cart.setItems(List.of(cake));
    cart.setTotalQuantity(2);
    cart.setTotalAmount(new BigDecimal("500000"));
    cart.setMaximumPreparationDays(2);
    cart.setCheckoutAllowed(true);
    when(carts.getCart(session)).thenReturn(cart);
  }

  @Test
  void rendersRealCartAndEditableProfileWithoutRequiringSavedAddress() throws Exception {
    var earliest = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(2);
    var result = mvc.perform(get("/checkout").session(session).with(user(customer)))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Bánh kem dâu &lt;đặc biệt&gt;")))
        .andExpect(content().string(containsString("500.000 ₫")))
        .andExpect(content().string(containsString("value=\"Phạm Nhật Hưng\"")))
        .andExpect(content().string(containsString("min=\"" + earliest + "\"")))
        .andExpect(content().string(containsString("productNotes[42]")))
        .andExpect(content().string(containsString("maxlength=\"200\"")))
        .andExpect(content().string(containsString("/assets/product-placeholder.svg")))
        .andExpect(content().string(containsString("/css/checkout.css")))
        .andExpect(content().string(containsString("/js/checkout.js")))
        .andExpect(content().string(containsString("Đơn hàng chưa được tạo.")))
        .andExpect(content().string(not(containsString("Bạn chưa lưu địa chỉ giao hàng."))))
        .andReturn();
    savePreview("checkout", result);
  }

  @Test
  void usesDefaultSavedAddressAndEscapesItsContent() throws Exception {
    when(addresses.findByAccountId(7L)).thenReturn(List.of(address(12L, false), address(13L, true)));
    var result = mvc.perform(get("/checkout").session(session).with(user(customer)))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("value=\"Người nhận 13\"")))
        .andExpect(content().string(containsString("data-note=\"Gọi trước &lt;khi giao&gt;\"")))
        .andExpect(content().string(containsString("Quản lý sổ địa chỉ")))
        .andReturn();
    savePreview("checkout-addresses", result);
  }

  @Test
  void selectsOnlyAnAddressOwnedByTheAuthenticatedAccount() throws Exception {
    when(addresses.findByAccountId(7L)).thenReturn(List.of(address(12L, false), address(13L, true)));
    mvc.perform(get("/checkout?addressId=12").session(session).with(user(customer)))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("value=\"Người nhận 12\"")));
    mvc.perform(get("/checkout?addressId=999").session(session).with(user(customer)))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Địa chỉ đã chọn không còn khả dụng")))
        .andExpect(content().string(containsString("value=\"Phạm Nhật Hưng\"")));
  }

  @Test
  void emptyCartShowsShoppingLinkWithoutRecipientForm() throws Exception {
    cart.setItems(List.of());
    cart.setCheckoutAllowed(false);
    var result = mvc.perform(get("/checkout").session(session).with(user(customer)))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Chưa có chiếc bánh nào ở đây.")))
        .andExpect(content().string(containsString("href=\"/products\"")))
        .andExpect(content().string(not(containsString("id=\"checkout-form\""))))
        .andReturn();
    savePreview("checkout-empty", result);
  }

  @Test
  void unavailableCartKeepsProductVisibleAndDisablesReview() throws Exception {
    cart.setCheckoutAllowed(false);
    cart.getItems().get(0).setAvailable(false);
    cart.getItems().get(0).setAvailabilityMessage("Sản phẩm đã ngừng bán");
    var result = mvc.perform(get("/checkout").session(session).with(user(customer)))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Giỏ bánh cần được cập nhật")))
        .andExpect(content().string(containsString("Sản phẩm đã ngừng bán")))
        .andExpect(content().string(containsString("disabled=\"disabled\"")))
        .andReturn();
    savePreview("checkout-unavailable", result);
  }

  @Test
  void guestMustLogInBeforeReadingCheckoutData() throws Exception {
    mvc.perform(get("/checkout")).andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));
    verifyNoInteractions(carts, accounts, addresses);
  }

  @Test
  void staffCannotReadCustomerCheckout() throws Exception {
    mvc.perform(get("/checkout").with(user("staff").roles("STAFF")))
        .andExpect(status().isForbidden());
    verifyNoInteractions(carts, accounts, addresses);
  }

  @Test
  void invalidPostKeepsReceiverValuesAndDoesNotCreateOrder() throws Exception {
    mvc.perform(validPost().param("receiverPhone", "123"))
        .andExpect(status().isOk())
        .andExpect(result -> org.junit.jupiter.api.Assertions.assertTrue(
            org.springframework.web.util.HtmlUtils.htmlUnescape(result.getResponse().getContentAsString())
                .contains("value=\"Người nhận riêng\"")))
        .andExpect(content().string(containsString("Số điện thoại không đúng định dạng")));
    verifyNoInteractions(submissions);
  }

  @Test
  void validPostRedirectsToThePersistedOrderResult() throws Exception {
    var order = new OrderDetailResponse();
    order.setId(55L);
    when(submissions.submit(eq(7L), eq(session), any())).thenReturn(order);
    mvc.perform(validPost()).andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/checkout/success/55"));
  }

  @Test
  void businessConflictKeepsNotesAndShowsCurrentCart() throws Exception {
    when(submissions.submit(eq(7L), eq(session), any()))
        .thenThrow(new ConflictException("Giá bánh đã thay đổi, vui lòng kiểm tra lại."));
    mvc.perform(validPost().param("productNotes[42]", "Ít ngọt"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Giá bánh đã thay đổi")))
        .andExpect(content().string(containsString("Ít ngọt")))
        .andExpect(result -> org.junit.jupiter.api.Assertions.assertTrue(
            org.springframework.web.util.HtmlUtils.htmlUnescape(result.getResponse().getContentAsString())
                .contains("value=\"Người nhận riêng\"")));
  }

  @Test
  void htmlOrOversizedProductNotesAreRejectedOnServer() throws Exception {
    mvc.perform(validPost().param("productNotes[42]", "<script>alert(1)</script>"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Lời nhắn cho bánh chỉ nhận văn bản")));
    mvc.perform(validPost().param("note", "x".repeat(201)))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Lời nhắn không được vượt quá 200 ký tự")));
    verifyNoInteractions(submissions);
  }

  @Test
  void placingOrderRequiresCsrf() throws Exception {
    mvc.perform(post("/checkout").session(session).with(user(customer)))
        .andExpect(status().isForbidden());
    verifyNoInteractions(submissions);
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder validPost() {
    return post("/checkout").session(session).with(user(customer)).with(csrf())
        .param("checkoutToken", java.util.UUID.randomUUID().toString())
        .param("receiverName", "Người nhận riêng").param("receiverPhone", "0912345678")
        .param("receiverAddress", "20 Lê Lợi, TP. Hồ Chí Minh")
        .param("deliveryDate", LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(3).toString());
  }

  private AccountAddressResponse address(Long id, boolean isDefault) {
    var address = new AccountAddressResponse();
    address.setId(id);
    address.setLabel(isDefault ? "Nhà riêng" : "Văn phòng");
    address.setReceiverName("Người nhận " + id);
    address.setReceiverPhone("0987654321");
    address.setAddress("25 Lê Lợi, Quận 1, TP. Hồ Chí Minh");
    address.setDeliveryNote("Gọi trước <khi giao>");
    address.setDefaultAddress(isDefault);
    return address;
  }

  private void savePreview(String name, MvcResult result) throws Exception {
    if (Boolean.getBoolean("checkout.preview")) {
      Path directory = Path.of("target", "checkout-preview");
      Files.createDirectories(directory);
      Files.writeString(directory.resolve(name + ".html"),
          result.getResponse().getContentAsString(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }
  }
}
