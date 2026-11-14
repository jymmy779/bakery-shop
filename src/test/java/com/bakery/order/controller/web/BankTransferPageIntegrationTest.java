package com.bakery.order.controller.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.bakery.account.entity.Account;
import com.bakery.account.entity.AccountStatus;
import com.bakery.account.entity.Role;
import com.bakery.account.repository.AccountRepository;
import com.bakery.account.security.CustomUserDetails;
import com.bakery.cart.service.impl.SessionCartServiceImpl;
import com.bakery.catalog.entity.Product;
import com.bakery.config.SecurityConfig;
import com.bakery.order.dto.BankTransferResponse;
import com.bakery.order.entity.Order;
import com.bakery.order.entity.OrderDetail;
import com.bakery.order.entity.OrderStatus;
import com.bakery.order.entity.Payment;
import com.bakery.order.entity.PaymentMethod;
import com.bakery.order.entity.PaymentStatus;
import com.bakery.order.repository.OrderDetailRepository;
import com.bakery.order.repository.OrderRepository;
import com.bakery.order.repository.OrderStatusHistoryRepository;
import com.bakery.order.repository.PaymentRepository;
import com.bakery.order.service.impl.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest({CheckoutController.class, OrderController.class})
@Import({SecurityConfig.class, CheckoutResultServiceImpl.class, CustomerOrderQueryServiceImpl.class,
    CheckoutOrderMapper.class, BankTransferServiceImpl.class, BankTransferProperties.class})
@TestPropertySource(properties = {
    "app.payment.bank-id=970436", "app.payment.bank-name=Ngân hàng kiểm thử",
    "app.payment.account-number=0000000000000", "app.payment.account-name=LUNE TEST ONLY",
    "app.payment.qr-template=qr_only"
})
class BankTransferPageIntegrationTest {

  @Autowired MockMvc mvc;
  @MockitoBean OrderRepository orders;
  @MockitoBean OrderDetailRepository details;
  @MockitoBean PaymentRepository payments;
  @MockitoBean OrderStatusHistoryRepository history;
  @MockitoBean AccountRepository accounts;
  @MockitoBean SessionCartServiceImpl carts;
  @MockitoBean CheckoutPageServiceImpl checkoutPages;
  @MockitoBean CheckoutSubmissionServiceImpl submissions;
  @MockitoSpyBean BankTransferProperties bank;
  private CustomUserDetails customer;
  private Order order;
  private Payment payment;

  @BeforeEach
  void setup() {
    var account = new Account();
    account.setId(7L);
    account.setFullName("Phạm Nhật Hưng");
    account.setEmail("hung@example.com");
    account.setRole(Role.ROLE_CUSTOMER);
    account.setStatus(AccountStatus.ACTIVE);
    account.setPasswordHash("unused");
    customer = new CustomUserDetails(account);
    order = new Order();
    order.setId(51L);
    order.setAccount(account);
    order.setOrderCode("LN123456789ABCDEFGHIJKLMNPQR");
    order.setStatus(OrderStatus.PENDING);
    order.setReceiverName(account.getFullName());
    order.setReceiverPhone("0912345678");
    order.setReceiverAddress("12 Nguyễn Huệ, Quận 1, TP. Hồ Chí Minh");
    order.setDeliveryDate(LocalDate.of(2026, 11, 18));
    order.setTotalAmount(new BigDecimal("500000"));
    ReflectionTestUtils.setField(order, "createdAt", LocalDateTime.of(2026, 11, 14, 9, 0));
    payment = new Payment();
    payment.setId(61L);
    payment.setOrder(order);
    payment.setMethod(PaymentMethod.BANK_TRANSFER);
    payment.setStatus(PaymentStatus.PENDING);
    payment.setAmount(order.getTotalAmount());
    var product = new Product();
    product.setId(42L);
    var detail = new OrderDetail();
    detail.setOrder(order);
    detail.setProduct(product);
    detail.setProductName("Bánh kem dâu");
    detail.setQuantity(2);
    detail.setUnitPrice(new BigDecimal("250000"));
    detail.setLineTotal(order.getTotalAmount());
    when(orders.findByIdAndAccount_Id(51L, 7L)).thenReturn(Optional.of(order));
    when(details.findByOrder_IdOrderByIdAsc(51L)).thenReturn(List.of(detail));
    when(payments.findByOrder_Id(51L)).thenReturn(Optional.of(payment));
    when(history.findByOrder_IdOrderByChangedAtAscIdAsc(51L)).thenReturn(List.of());
  }

  @Test
  void receiptAndOrderDetailShowTheSameExactTransferInstructions() throws Exception {
    for (String path : List.of("/checkout/success/51", "/orders/51")) {
      var result = mvc.perform(get(path).with(user(customer))).andExpect(status().isOk())
          .andExpect(content().string(containsString("Ngân hàng kiểm thử")))
          .andExpect(content().string(containsString("value=\"0000000000000\"")))
          .andExpect(content().string(containsString("value=\"500000\"")))
          .andExpect(content().string(containsString("referrerpolicy=\"no-referrer\"")))
          .andExpect(content().string(containsString("data-copy-target=\"transfer-content\"")))
          .andExpect(content().string(containsString("LUNE sẽ đối chiếu"))).andReturn();
      var transfer = transfer(result);
      assertTrue(transfer.isAvailable());
      assertEquals(order.getOrderCode(), transfer.getContent());
      assertTrue(transfer.getQrImageUrl().contains(".svg?amount=500000&addInfo=" + order.getOrderCode()));
      savePreview(path.startsWith("/checkout") ? "receipt" : "detail", result);
    }
    verify(orders, never()).save(any());
    verify(payments, never()).save(any());
    assertEquals(PaymentStatus.PENDING, payment.getStatus());
  }

  @Test
  void clientCannotChangeAmountBankOrTransferContentThroughQueryParameters() throws Exception {
    var result = mvc.perform(get("/orders/51?amount=1&bankId=EVIL&accountNumber=123&content=WRONG&accountId=99")
            .with(user(customer))).andExpect(status().isOk()).andReturn();
    var transfer = transfer(result);
    assertEquals("500000", transfer.getAmount());
    assertEquals("0000000000000", transfer.getAccountNumber());
    assertEquals(order.getOrderCode(), transfer.getContent());
    assertFalse(transfer.getQrImageUrl().contains("EVIL"));
    verify(orders).findByIdAndAccount_Id(51L, 7L);
  }

  @Test
  void missingBankConfigurationDoesNotBreakOrderPagesOrRenderDummyQr() throws Exception {
    when(bank.getBankId()).thenReturn("");
    for (String path : List.of("/checkout/success/51", "/orders/51")) {
      var result = mvc.perform(get(path).with(user(customer))).andExpect(status().isOk())
          .andExpect(content().string(containsString("đang được cập nhật")))
          .andExpect(content().string(not(containsString("data-transfer-qr"))))
          .andExpect(content().string(not(containsString("0000000000000")))).andReturn();
      savePreview(path.startsWith("/checkout") ? "unconfigured-receipt" : "unconfigured-detail", result);
    }
  }

  @Test
  void paidAndCancelledOrdersHaveNoQrOrCopyControls() throws Exception {
    payment.setStatus(PaymentStatus.PAID);
    var paid = mvc.perform(get("/orders/51").with(user(customer))).andExpect(status().isOk())
        .andExpect(content().string(containsString("LUNE đã ghi nhận thanh toán")))
        .andExpect(content().string(not(containsString("data-copy-target")))).andReturn();
    savePreview("paid", paid);
    order.setStatus(OrderStatus.CANCELLED);
    payment.setStatus(PaymentStatus.PENDING);
    var cancelled = mvc.perform(get("/checkout/success/51").with(user(customer))).andExpect(status().isOk())
        .andExpect(content().string(containsString("Bạn không cần chuyển khoản cho đơn này")))
        .andExpect(content().string(not(containsString("data-transfer-qr")))).andReturn();
    savePreview("cancelled", cancelled);
  }

  @Test
  void inconsistentPaymentAmountShowsContactMessageWithoutOfferingAnIncorrectQr() throws Exception {
    payment.setAmount(BigDecimal.ONE);
    mvc.perform(get("/orders/51").with(user(customer))).andExpect(status().isOk())
        .andExpect(content().string(containsString("Thông tin thanh toán cần được kiểm tra")))
        .andExpect(content().string(not(containsString("data-transfer-qr"))));
  }

  @Test
  void ownershipIsCheckedBeforeReadingPaymentDetailsOnBothPages() throws Exception {
    for (String path : List.of("/checkout/success/52", "/orders/52")) {
      mvc.perform(get(path).with(user(customer))).andExpect(status().isNotFound());
    }
    verify(orders, times(2)).findByIdAndAccount_Id(52L, 7L);
    verifyNoInteractions(payments, details, history);
  }

  @Test
  void guestStaffAndAdminCannotAccessCustomerTransferInstructions() throws Exception {
    for (String path : List.of("/checkout/success/51", "/orders/51")) {
      mvc.perform(get(path)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
      for (String role : List.of("STAFF", "ADMIN")) {
        mvc.perform(get(path).with(user("internal").roles(role))).andExpect(status().isForbidden());
      }
    }
    verifyNoInteractions(orders, payments, details, history);
  }

  private BankTransferResponse transfer(MvcResult result) {
    return (BankTransferResponse) result.getModelAndView().getModel().get("bankTransfer");
  }

  private void savePreview(String name, MvcResult result) throws Exception {
    if (!Boolean.getBoolean("payment.preview")) return;
    var directory = Path.of("target", "payment-preview");
    Files.createDirectories(directory);
    Files.writeString(directory.resolve(name + ".html"), result.getResponse().getContentAsString(StandardCharsets.UTF_8));
  }
}
