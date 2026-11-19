package com.bakery.order.controller.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.bakery.account.entity.Account;
import com.bakery.account.entity.AccountStatus;
import com.bakery.account.entity.Role;
import com.bakery.account.repository.AccountRepository;
import com.bakery.account.security.CustomUserDetails;
import com.bakery.catalog.entity.Category;
import com.bakery.catalog.entity.Product;
import com.bakery.catalog.entity.ProductStatus;
import com.bakery.catalog.repository.CategoryRepository;
import com.bakery.catalog.repository.ProductRepository;
import com.bakery.common.exception.ConflictException;
import com.bakery.common.exception.InvalidOperationException;
import com.bakery.common.exception.ResourceNotFoundException;
import com.bakery.order.dto.PaymentConfirmationRequest;
import com.bakery.order.entity.*;
import com.bakery.order.repository.*;
import com.bakery.order.service.PaymentService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Isolated H2 transaction/MVC checks; never uses the developer's MySQL database. */
@EnabledIfSystemProperty(named = "payment.confirmationTests", matches = "true")
@ActiveProfiles("payment-confirmation-test")
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:paymentConfirmation;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
    "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.open-in-view=false",
    "spring.sql.init.mode=never", "app.bootstrap-admin.enabled=false", "logging.level.root=WARN",
    "app.payment.bank-id=970436", "app.payment.bank-name=Ngân hàng kiểm thử",
    "app.payment.account-number=0000000000000", "app.payment.account-name=LUNE TEST ONLY"
})
@AutoConfigureMockMvc
class PaymentConfirmationIntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired AccountRepository accounts;
  @Autowired OrderRepository orders;
  @Autowired PaymentRepository payments;
  @Autowired OrderDetailRepository details;
  @MockitoSpyBean OrderStatusHistoryRepository history;
  @Autowired PaymentService service;
  @Autowired CategoryRepository categories;
  @Autowired ProductRepository products;

  @ParameterizedTest
  @EnumSource(value = Role.class, names = {"ROLE_STAFF", "ROLE_ADMIN"})
  void staffAndAdminConfirmAtomicallyAndCustomerSeesCommittedSuccess(Role role) throws Exception {
    var customer = account(Role.ROLE_CUSTOMER);
    var staff = account(role);
    var order = order(customer, OrderStatus.PENDING);
    var product = item(order);
    var before = mvc.perform(get("/orders/{id}/payment-status", order.getId())
            .with(user(new CustomUserDetails(customer))))
        .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.orderStatus").value("PENDING"))
        .andExpect(jsonPath("$.paymentStatus").value("PENDING")).andReturn();
    assertFalse(before.getResponse().getContentAsString().contains(customer.getEmail()));
    var pending = mvc.perform(get("/cms/orders/{id}", order.getId()).with(user(new CustomUserDetails(staff))))
        .andExpect(status().isOk()).andExpect(content().string(containsString("Xác nhận đã nhận đủ tiền")))
        .andExpect(content().string(containsString("name=\"_csrf\""))).andReturn();
    preview("cms-pending", pending);
    if (Boolean.getBoolean("payment.preview")) {
      preview("customer-pending", mvc.perform(get("/orders/{id}", order.getId())
          .with(user(new CustomUserDetails(customer)))).andExpect(status().isOk()).andReturn());
      preview("receipt-pending", mvc.perform(get("/checkout/success/{id}", order.getId())
          .with(user(new CustomUserDetails(customer)))).andExpect(status().isOk()).andReturn());
    }
    mvc.perform(post("/cms/orders/{id}/payment/confirm", order.getId()).with(user(new CustomUserDetails(staff)))
            .with(csrf()).param("bankReference", "  BANK-TEST-001  ")
            .param("confirmedByAccountId", customer.getId().toString()).param("status", "CANCELLED"))
        .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/cms/orders/" + order.getId()))
        .andExpect(flash().attribute("successMessage", containsString("thành công")));
    var payment = payments.findByOrder_Id(order.getId()).orElseThrow();
    assertEquals(PaymentStatus.PAID, payment.getStatus());
    assertEquals("BANK-TEST-001", payment.getBankReference());
    assertEquals(staff.getId(), payment.getConfirmedBy().getId());
    assertNotNull(payment.getPaidAt());
    assertEquals(OrderStatus.CONFIRMED, orders.findById(order.getId()).orElseThrow().getStatus());
    var events = history.findByOrder_IdOrderByChangedAtAscIdAsc(order.getId());
    assertEquals(1, events.size());
    assertEquals(OrderStatus.PENDING, events.get(0).getFromStatus());
    assertEquals(OrderStatus.CONFIRMED, events.get(0).getToStatus());
    assertEquals(payment.getPaidAt(), events.get(0).getChangedAt());
    assertEquals(staff.getId(), events.get(0).getChangedBy().getId());
    assertEquals(10, products.findById(product.getId()).orElseThrow().getStockQuantity());
    mvc.perform(get("/orders/{id}/payment-status", order.getId()).with(user(new CustomUserDetails(customer))))
        .andExpect(status().isOk()).andExpect(jsonPath("$.orderStatus").value("CONFIRMED"))
        .andExpect(jsonPath("$.paymentStatus").value("PAID")).andExpect(jsonPath("$.paidAt").isNotEmpty());
    var paid = mvc.perform(get("/orders/{id}", order.getId()).with(user(new CustomUserDetails(customer))))
        .andExpect(status().isOk()).andExpect(content().string(containsString("Thanh toán thành công!")))
        .andExpect(content().string(not(containsString("data-transfer-qr"))))
        .andExpect(content().string(not(containsString("data-payment-tracking")))).andReturn();
    preview("customer-paid", paid);
    var cmsPaid = mvc.perform(get("/cms/orders/{id}", order.getId()).with(user(new CustomUserDetails(staff))))
        .andExpect(status().isOk()).andExpect(content().string(containsString(staff.getFullName())))
        .andExpect(content().string(not(containsString("class=\"cms-order-confirm\"")))).andReturn();
    preview("cms-paid", cmsPaid);
  }

  @Test
  void adminSearchAndPaymentFiltersShowOrdersAcrossCustomers() throws Exception {
    var staff = account(Role.ROLE_STAFF);
    var first = order(account(Role.ROLE_CUSTOMER), OrderStatus.PENDING);
    var second = order(account(Role.ROLE_CUSTOMER), OrderStatus.PENDING);
    String prefix = "LN" + UUID.randomUUID().toString().replace("-", "").substring(0, 15).toUpperCase();
    first.setOrderCode(prefix + "A"); second.setOrderCode(prefix + "B");
    orders.saveAllAndFlush(List.of(first, second));
    service.confirmBankTransfer(second.getId(), staff.getId(), new PaymentConfirmationRequest());
    var all = mvc.perform(get("/cms/orders").param("search", prefix).with(user(new CustomUserDetails(staff))))
        .andExpect(status().isOk()).andExpect(content().string(containsString(first.getOrderCode())))
        .andExpect(content().string(containsString(second.getOrderCode()))).andReturn();
    preview("cms-list", all);
    mvc.perform(get("/cms/orders").param("search", prefix.toLowerCase()).param("paymentStatus", "PENDING")
            .param("status", "PENDING").with(user(new CustomUserDetails(staff))))
        .andExpect(status().isOk()).andExpect(content().string(containsString(first.getOrderCode())))
        .andExpect(content().string(not(containsString(second.getOrderCode()))));
    mvc.perform(get("/cms/orders?search=NOTFOUND&page=999").with(user(new CustomUserDetails(staff))))
        .andExpect(status().isOk()).andExpect(content().string(containsString("Không tìm thấy đơn hàng phù hợp")));
  }

  @Test
  void guestCustomerAndCsrfCannotConfirm() throws Exception {
    var customer = account(Role.ROLE_CUSTOMER);
    var staff = account(Role.ROLE_STAFF);
    var order = order(customer, OrderStatus.PENDING);
    String path = "/cms/orders/" + order.getId() + "/payment/confirm";
    mvc.perform(post(path).with(csrf())).andExpect(status().is3xxRedirection());
    mvc.perform(post(path).with(csrf()).with(user(new CustomUserDetails(customer)))).andExpect(status().isForbidden());
    mvc.perform(post(path).with(user(new CustomUserDetails(staff)))).andExpect(status().isForbidden());
    mvc.perform(get("/cms/orders/" + order.getId()).with(user(new CustomUserDetails(customer))))
        .andExpect(status().isForbidden());
    assertPending(order);
  }

  @Test
  void serviceAlsoRejectsCustomerAndCurrentlyLockedStaff() {
    var customer = account(Role.ROLE_CUSTOMER);
    var staff = account(Role.ROLE_STAFF);
    var order = order(customer, OrderStatus.PENDING);
    staff.setStatus(AccountStatus.LOCKED); accounts.saveAndFlush(staff);
    for (var actor : List.of(customer, staff)) {
      assertThrows(AccessDeniedException.class,
          () -> service.confirmBankTransfer(order.getId(), actor.getId(), new PaymentConfirmationRequest()));
    }
    assertPending(order);
  }

  @Test
  void duplicatePostDoesNotChangeFirstReferenceOrAddHistory() throws Exception {
    var staff = account(Role.ROLE_STAFF);
    var order = order(account(Role.ROLE_CUSTOMER), OrderStatus.PENDING);
    var request = new PaymentConfirmationRequest(); request.setBankReference("FIRST");
    service.confirmBankTransfer(order.getId(), staff.getId(), request);
    mvc.perform(post("/cms/orders/{id}/payment/confirm", order.getId()).with(csrf())
            .with(user(new CustomUserDetails(staff))).param("bankReference", "SECOND"))
        .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("errorMessage", containsString("chờ xử lý")));
    assertEquals("FIRST", payments.findByOrder_Id(order.getId()).orElseThrow().getBankReference());
    assertEquals(1, history.findByOrder_IdOrderByChangedAtAscIdAsc(order.getId()).size());
  }

  @ParameterizedTest
  @EnumSource(value = OrderStatus.class, names = {"CONFIRMED", "DELIVERING", "DELIVERED", "CANCELLED"})
  void nonPendingOrdersCannotBeConfirmed(OrderStatus status) {
    var staff = account(Role.ROLE_STAFF);
    var order = order(account(Role.ROLE_CUSTOMER), status);
    assertThrows(ConflictException.class,
        () -> service.confirmBankTransfer(order.getId(), staff.getId(), new PaymentConfirmationRequest()));
    assertEquals(PaymentStatus.PENDING, payments.findByOrder_Id(order.getId()).orElseThrow().getStatus());
    assertTrue(history.findByOrder_IdOrderByChangedAtAscIdAsc(order.getId()).isEmpty());
  }

  @Test
  void inconsistentAmountsAndAlreadyPaidPaymentsAreRejected() {
    var staff = account(Role.ROLE_STAFF);
    for (String amount : List.of("0", "899999", "900001")) {
      var order = order(account(Role.ROLE_CUSTOMER), OrderStatus.PENDING);
      var payment = payments.findByOrder_Id(order.getId()).orElseThrow();
      payment.setAmount(new BigDecimal(amount)); payments.saveAndFlush(payment);
      assertThrows(ConflictException.class,
          () -> service.confirmBankTransfer(order.getId(), staff.getId(), new PaymentConfirmationRequest()));
      assertPending(order);
    }
    var order = order(account(Role.ROLE_CUSTOMER), OrderStatus.PENDING);
    var payment = payments.findByOrder_Id(order.getId()).orElseThrow();
    payment.setStatus(PaymentStatus.PAID); payments.saveAndFlush(payment);
    assertThrows(ConflictException.class,
        () -> service.confirmBankTransfer(order.getId(), staff.getId(), new PaymentConfirmationRequest()));
    assertEquals(OrderStatus.PENDING, orders.findById(order.getId()).orElseThrow().getStatus());
  }

  @Test
  void overlongAndNullRequestsAreRejectedBeforeMutation() throws Exception {
    var staff = account(Role.ROLE_STAFF);
    var order = order(account(Role.ROLE_CUSTOMER), OrderStatus.PENDING);
    var request = new PaymentConfirmationRequest(); request.setBankReference("x".repeat(101));
    assertThrows(InvalidOperationException.class, () -> service.confirmBankTransfer(order.getId(), staff.getId(), request));
    assertThrows(InvalidOperationException.class, () -> service.confirmBankTransfer(order.getId(), staff.getId(), null));
    mvc.perform(post("/cms/orders/{id}/payment/confirm", order.getId()).with(csrf())
            .with(user(new CustomUserDetails(staff))).param("bankReference", request.getBankReference()))
        .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("errorMessage", containsString("100")));
    assertPending(order);
  }

  @Test
  void historyFailureRollsBackPaymentAndOrder() {
    var staff = account(Role.ROLE_STAFF);
    var order = order(account(Role.ROLE_CUSTOMER), OrderStatus.PENDING);
    doThrow(new IllegalStateException("simulated history failure")).when(history).saveAndFlush(any());
    try {
      assertThrows(IllegalStateException.class,
          () -> service.confirmBankTransfer(order.getId(), staff.getId(), new PaymentConfirmationRequest()));
    } finally { reset(history); }
    assertPending(order);
    var payment = payments.findByOrder_Id(order.getId()).orElseThrow();
    assertNull(payment.getPaidAt()); assertNull(payment.getConfirmedBy());
  }

  @Test
  void simultaneousConfirmationsCommitExactlyOnce() throws Exception {
    var staff = account(Role.ROLE_STAFF);
    var order = order(account(Role.ROLE_CUSTOMER), OrderStatus.PENDING);
    var ready = new CountDownLatch(2); var start = new CountDownLatch(1);
    var pool = Executors.newFixedThreadPool(2);
    try {
      var jobs = java.util.stream.IntStream.range(0, 2).mapToObj(index -> pool.submit(() -> {
        ready.countDown(); assertTrue(start.await(10, TimeUnit.SECONDS));
        try { service.confirmBankTransfer(order.getId(), staff.getId(), new PaymentConfirmationRequest()); return true; }
        catch (ConflictException expected) { return false; }
      })).toList();
      assertTrue(ready.await(10, TimeUnit.SECONDS)); start.countDown();
      int successes = 0;
      for (var job : jobs) if (job.get(20, TimeUnit.SECONDS)) successes++;
      assertEquals(1, successes);
    } finally { pool.shutdownNow(); }
    assertEquals(1, history.findByOrder_IdOrderByChangedAtAscIdAsc(order.getId()).size());
    assertEquals(PaymentStatus.PAID, payments.findByOrder_Id(order.getId()).orElseThrow().getStatus());
    assertEquals(OrderStatus.CONFIRMED, orders.findById(order.getId()).orElseThrow().getStatus());
  }

  @Test
  void statusIsOwnerOnlyAndCannotBeUsedToMarkPaymentPaid() throws Exception {
    var customer = account(Role.ROLE_CUSTOMER);
    var order = order(customer, OrderStatus.PENDING);
    var other = account(Role.ROLE_CUSTOMER);
    String path = "/orders/" + order.getId() + "/payment-status";
    mvc.perform(get(path).param("accountId", customer.getId().toString()).with(user(new CustomUserDetails(other))))
        .andExpect(status().isNotFound());
    mvc.perform(get("/orders/9223372036854775807/payment-status").with(user(new CustomUserDetails(customer))))
        .andExpect(status().isNotFound());
    mvc.perform(get(path)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
    mvc.perform(get(path).with(user(new CustomUserDetails(account(Role.ROLE_STAFF)))))
        .andExpect(status().isForbidden());
    mvc.perform(post(path).with(csrf()).with(user(new CustomUserDetails(customer))).param("paymentStatus", "PAID"))
        .andExpect(status().isMethodNotAllowed());
    assertPending(order);
  }

  @Test
  void missingOrderAndPaymentDoNotCreateConfirmation() {
    var staff = account(Role.ROLE_STAFF);
    assertThrows(ResourceNotFoundException.class,
        () -> service.confirmBankTransfer(Long.MAX_VALUE, staff.getId(), new PaymentConfirmationRequest()));
    var order = order(account(Role.ROLE_CUSTOMER), OrderStatus.PENDING);
    payments.delete(payments.findByOrder_Id(order.getId()).orElseThrow());
    assertThrows(ResourceNotFoundException.class,
        () -> service.confirmBankTransfer(order.getId(), staff.getId(), new PaymentConfirmationRequest()));
    assertEquals(OrderStatus.PENDING, orders.findById(order.getId()).orElseThrow().getStatus());
  }

  @Test
  void bankReferenceIsEscapedInCmsAndCustomerPages() throws Exception {
    var staff = account(Role.ROLE_STAFF); var customer = account(Role.ROLE_CUSTOMER);
    var order = order(customer, OrderStatus.PENDING);
    var request = new PaymentConfirmationRequest(); request.setBankReference("<script>alert('bank')</script>");
    service.confirmBankTransfer(order.getId(), staff.getId(), request);
    for (var path : List.of("/cms/orders/", "/orders/")) {
      mvc.perform(get(path + order.getId()).with(user(new CustomUserDetails(path.contains("cms") ? staff : customer))))
          .andExpect(status().isOk()).andExpect(content().string(containsString("&lt;script&gt;")))
          .andExpect(content().string(not(containsString("<script>alert('bank')"))));
    }
  }

  private void assertPending(Order order) {
    assertEquals(OrderStatus.PENDING, orders.findById(order.getId()).orElseThrow().getStatus());
    assertEquals(PaymentStatus.PENDING, payments.findByOrder_Id(order.getId()).orElseThrow().getStatus());
    assertTrue(history.findByOrder_IdOrderByChangedAtAscIdAsc(order.getId()).isEmpty());
  }

  private Account account(Role role) {
    var account = new Account(); account.setEmail(UUID.randomUUID() + "@payment.test");
    account.setPasswordHash("unused"); account.setFullName(role == Role.ROLE_CUSTOMER ? "Khách kiểm thử" : "Nhân viên đối soát");
    account.setRole(role); account.setStatus(AccountStatus.ACTIVE);
    return accounts.saveAndFlush(account);
  }

  private Order order(Account customer, OrderStatus status) {
    var order = new Order(); order.setAccount(customer);
    order.setOrderCode("LN" + UUID.randomUUID().toString().replace("-", "").substring(0, 25).toUpperCase());
    order.setReceiverName(customer.getFullName()); order.setReceiverPhone("0912345678");
    order.setReceiverAddress("12 Nguyễn Huệ, Phường Bến Nghé, TP. Hồ Chí Minh");
    order.setDeliveryDate(LocalDate.of(2026, 11, 18)); order.setDeliveryTime(LocalTime.of(15, 30));
    order.setTotalAmount(new BigDecimal("900000")); order.setStatus(status); orders.saveAndFlush(order);
    var payment = new Payment(); payment.setOrder(order); payment.setMethod(PaymentMethod.BANK_TRANSFER);
    payment.setStatus(PaymentStatus.PENDING); payment.setAmount(order.getTotalAmount()); payments.saveAndFlush(payment);
    return order;
  }

  private Product item(Order order) {
    var category = new Category(); category.setName("Payment " + UUID.randomUUID()); category.setActive(true);
    categories.saveAndFlush(category);
    var product = new Product(); product.setName("Bánh kem dâu"); product.setCategory(category);
    product.setDescription("Bánh cho kiểm thử đối soát thanh toán");
    product.setPrice(new BigDecimal("450000")); product.setStatus(ProductStatus.SELLING); product.setStockQuantity(10);
    products.saveAndFlush(product);
    var detail = new OrderDetail(); detail.setOrder(order); detail.setProduct(product); detail.setProductName(product.getName());
    detail.setUnitPrice(product.getPrice()); detail.setQuantity(2); detail.setLineTotal(order.getTotalAmount());
    detail.setNote("Ít ngọt, viết chữ Chúc mừng"); details.saveAndFlush(detail);
    return product;
  }

  private void preview(String name, MvcResult result) throws Exception {
    if (!Boolean.getBoolean("payment.preview")) return;
    var directory = Path.of("target", "payment-preview"); Files.createDirectories(directory);
    Files.writeString(directory.resolve(name + ".html"), result.getResponse().getContentAsString(StandardCharsets.UTF_8));
  }
}
