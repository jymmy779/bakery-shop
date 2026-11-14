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
import com.bakery.order.dto.CancelOrderRequest;
import com.bakery.order.dto.PaymentConfirmationRequest;
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
import com.bakery.order.service.PaymentService;
import com.bakery.order.service.impl.CustomerOrderCancellationServiceImpl;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
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

/** Opt-in isolated H2 checks; never writes to the developer's MySQL database. */
@EnabledIfSystemProperty(named = "order.cancellationTests", matches = "true")
@ActiveProfiles("order-cancellation-test")
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:orderCancellation;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
    "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.open-in-view=false",
    "spring.sql.init.mode=never", "app.bootstrap-admin.enabled=false", "logging.level.root=WARN"
})
@AutoConfigureMockMvc
class OrderCancellationIntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired AccountRepository accounts;
  @Autowired OrderRepository orders;
  @Autowired PaymentRepository payments;
  @Autowired OrderDetailRepository details;
  @MockitoSpyBean OrderStatusHistoryRepository history;
  @Autowired CategoryRepository categories;
  @Autowired ProductRepository products;
  @Autowired CustomerOrderCancellationServiceImpl cancellations;
  @Autowired PaymentService confirmations;

  @Test
  void customerCancelsWithReasonAndRestoresStockAndHistoryTogether() throws Exception {
    var customer = account(Role.ROLE_CUSTOMER);
    var order = order(customer);
    var first = product(10); var second = product(7);
    item(order, first, 2); item(order, second, 3);
    var pending = mvc.perform(get("/orders/{id}", order.getId()).with(user(new CustomUserDetails(customer))))
        .andExpect(status().isOk()).andExpect(content().string(containsString("data-order-cancellation")))
        .andExpect(content().string(containsString("name=\"_csrf\""))).andReturn();
    preview("pending", pending);
    mvc.perform(post("/orders/{id}/cancel", order.getId()).with(csrf()).with(user(new CustomUserDetails(customer)))
            .param("reason", "  Tôi muốn đổi ngày nhận bánh.  ").param("status", "DELIVERED")
            .param("accountId", "99999"))
        .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/orders/" + order.getId()))
        .andExpect(flash().attribute("cancelSuccessMessage", "Hủy đơn hàng thành công."));
    var cancelled = orders.findById(order.getId()).orElseThrow();
    assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
    assertEquals("Tôi muốn đổi ngày nhận bánh.", cancelled.getCancellationReason());
    assertNotNull(cancelled.getCancelledAt());
    assertEquals(12, stock(first)); assertEquals(10, stock(second));
    var payment = payments.findByOrder_Id(order.getId()).orElseThrow();
    assertEquals(PaymentStatus.PENDING, payment.getStatus());
    assertNull(payment.getPaidAt()); assertNull(payment.getConfirmedBy());
    var events = history.findByOrder_IdOrderByChangedAtAscIdAsc(order.getId());
    assertEquals(1, events.size());
    assertEquals(OrderStatus.PENDING, events.get(0).getFromStatus());
    assertEquals(OrderStatus.CANCELLED, events.get(0).getToStatus());
    assertEquals(customer.getId(), events.get(0).getChangedBy().getId());
    assertEquals(cancelled.getCancelledAt(), events.get(0).getChangedAt());
    var result = mvc.perform(get("/orders/{id}", order.getId()).with(user(new CustomUserDetails(customer)))
            .flashAttr("cancelSuccessMessage", "Hủy đơn hàng thành công."))
        .andExpect(status().isOk()).andExpect(content().string(containsString("Không cần thanh toán")))
        .andExpect(content().string(containsString("Tôi muốn đổi ngày nhận bánh.")))
        .andExpect(content().string(not(containsString("data-order-cancellation"))))
        .andExpect(content().string(not(containsString("data-transfer-qr"))))
        .andExpect(content().string(not(containsString("data-payment-tracking")))).andReturn();
    preview("cancelled", result);
    mvc.perform(get("/orders/{id}/payment-status", order.getId()).with(user(new CustomUserDetails(customer))))
        .andExpect(status().isOk()).andExpect(jsonPath("$.orderStatus").value("CANCELLED"));
    mvc.perform(get("/orders?status=CANCELLED").with(user(new CustomUserDetails(customer))))
        .andExpect(status().isOk()).andExpect(content().string(containsString(order.getOrderCode())))
        .andExpect(content().string(containsString("Không cần thanh toán")));
    mvc.perform(get("/cms/orders/{id}", order.getId()).with(user(new CustomUserDetails(account(Role.ROLE_STAFF)))))
        .andExpect(status().isOk()).andExpect(content().string(containsString("Không cần thanh toán")))
        .andExpect(content().string(not(containsString("class=\"cms-order-confirm\""))));
  }

  @Test
  void invalidReasonRoundTripsWithFieldErrorAndDoesNotCancel() throws Exception {
    var customer = account(Role.ROLE_CUSTOMER); var order = order(customer); var product = product(10);
    item(order, product, 2);
    for (String reason : List.of("", "   ", "x".repeat(501))) {
      var post = mvc.perform(post("/orders/{id}/cancel", order.getId()).with(csrf())
              .with(user(new CustomUserDetails(customer))).param("reason", reason))
          .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("cancelErrorMessage"))
          .andExpect(flash().attributeExists("org.springframework.validation.BindingResult.cancellation"))
          .andReturn();
      var invalid = mvc.perform(get("/orders/{id}", order.getId()).with(user(new CustomUserDetails(customer)))
              .flashAttrs(post.getFlashMap()))
          .andExpect(status().isOk()).andExpect(content().string(containsString("data-cancellation-invalid=\"true\"")))
          .andExpect(content().string(containsString("aria-invalid=\"true\""))).andReturn();
      preview("invalid", invalid);
      assertPending(order); assertEquals(10, stock(product));
    }
    assertThrows(InvalidOperationException.class, () -> cancellations.cancel(customer.getId(), order.getId(), null));
  }

  @Test
  void maximumReasonAndStoredMarkupAreHandledWithoutInjection() throws Exception {
    var customer = account(Role.ROLE_CUSTOMER); var order = order(customer);
    item(order, product(10), 2);
    String reason = "<script>alert('cancel')</script>" + "x".repeat(469);
    assertEquals(500, reason.length());
    cancellations.cancel(customer.getId(), order.getId(), request(reason));
    assertEquals(reason, orders.findById(order.getId()).orElseThrow().getCancellationReason());
    assertEquals(reason, history.findByOrder_IdOrderByChangedAtAscIdAsc(order.getId()).get(0).getNote());
    mvc.perform(get("/orders/{id}", order.getId()).with(user(new CustomUserDetails(customer))))
        .andExpect(status().isOk()).andExpect(content().string(containsString("&lt;script&gt;")))
        .andExpect(content().string(not(containsString("<script>alert('cancel')"))));
  }

  @Test
  void foreignAndMissingOrdersAreNotDisclosedOrMutated() throws Exception {
    var owner = account(Role.ROLE_CUSTOMER); var other = account(Role.ROLE_CUSTOMER);
    var order = order(owner); var product = product(10); item(order, product, 2);
    for (Long id : List.of(order.getId(), Long.MAX_VALUE)) {
      mvc.perform(post("/orders/{id}/cancel", id).with(csrf()).with(user(new CustomUserDetails(other)))
              .param("reason", "Hủy đơn").param("accountId", owner.getId().toString()))
          .andExpect(status().isNotFound()).andExpect(content().string(not(containsString(order.getOrderCode()))));
    }
    assertPending(order); assertEquals(10, stock(product));
  }

  @Test
  void guestStaffAdminAndRequestsWithoutCsrfCannotCancel() throws Exception {
    var owner = account(Role.ROLE_CUSTOMER); var order = order(owner); var product = product(10);
    item(order, product, 2); String path = "/orders/" + order.getId() + "/cancel";
    mvc.perform(post(path).with(csrf()).param("reason", "Hủy đơn"))
        .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
    mvc.perform(post(path).with(user(new CustomUserDetails(owner))).param("reason", "Hủy đơn"))
        .andExpect(status().isForbidden());
    for (Role role : List.of(Role.ROLE_STAFF, Role.ROLE_ADMIN)) {
      var actor = account(role);
      mvc.perform(post(path).with(csrf()).with(user(new CustomUserDetails(actor))).param("reason", "Hủy đơn"))
          .andExpect(status().isForbidden());
      assertThrows(AccessDeniedException.class, () -> cancellations.cancel(actor.getId(), order.getId(), request("Hủy")));
    }
    owner.setStatus(AccountStatus.LOCKED); accounts.saveAndFlush(owner);
    assertThrows(AccessDeniedException.class, () -> cancellations.cancel(owner.getId(), order.getId(), request("Hủy")));
    assertPending(order); assertEquals(10, stock(product));
  }

  @ParameterizedTest
  @EnumSource(value = OrderStatus.class, names = {"CONFIRMED", "DELIVERING", "DELIVERED", "CANCELLED"})
  void rejectsNonPendingOrdersAndHidesCancellationControls(OrderStatus status) throws Exception {
    var customer = account(Role.ROLE_CUSTOMER); var order = order(customer); var product = product(10);
    item(order, product, 2); order.setStatus(status); orders.saveAndFlush(order);
    assertThrows(ConflictException.class, () -> cancellations.cancel(customer.getId(), order.getId(), request("Hủy")));
    mvc.perform(get("/orders/{id}", order.getId()).with(user(new CustomUserDetails(customer))))
        .andExpect(status().isOk()).andExpect(content().string(not(containsString("data-order-cancellation"))));
    assertEquals(status, orders.findById(order.getId()).orElseThrow().getStatus());
    assertEquals(10, stock(product));
    assertTrue(history.findByOrder_IdOrderByChangedAtAscIdAsc(order.getId()).isEmpty());
  }

  @Test
  void paidPaymentOnPendingOrderIsRejected() throws Exception {
    var customer = account(Role.ROLE_CUSTOMER); var order = order(customer); var product = product(10);
    item(order, product, 2); var payment = payments.findByOrder_Id(order.getId()).orElseThrow();
    payment.setStatus(PaymentStatus.PAID); payments.saveAndFlush(payment);
    assertThrows(ConflictException.class, () -> cancellations.cancel(customer.getId(), order.getId(), request("Hủy")));
    mvc.perform(get("/orders/{id}", order.getId()).with(user(new CustomUserDetails(customer))))
        .andExpect(status().isOk()).andExpect(content().string(not(containsString("data-order-cancellation"))));
    assertEquals(OrderStatus.PENDING, orders.findById(order.getId()).orElseThrow().getStatus());
    assertEquals(10, stock(product));
  }

  @Test
  void cancelledOrderCannotBeConfirmedOrRestockedTwice() throws Exception {
    var customer = account(Role.ROLE_CUSTOMER); var order = order(customer); var product = product(10);
    item(order, product, 2); cancellations.cancel(customer.getId(), order.getId(), request("Lý do đầu tiên"));
    var duplicate = mvc.perform(post("/orders/{id}/cancel", order.getId()).with(csrf())
            .with(user(new CustomUserDetails(customer))).param("reason", "Lý do gửi trùng"))
        .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("cancelErrorMessage")).andReturn();
    preview("conflict", mvc.perform(get("/orders/{id}", order.getId()).with(user(new CustomUserDetails(customer)))
        .flashAttrs(duplicate.getFlashMap())).andExpect(status().isOk()).andReturn());
    var staff = account(Role.ROLE_STAFF);
    assertThrows(ConflictException.class,
        () -> confirmations.confirmBankTransfer(order.getId(), staff.getId(), new PaymentConfirmationRequest()));
    assertEquals("Lý do đầu tiên", orders.findById(order.getId()).orElseThrow().getCancellationReason());
    assertEquals(12, stock(product));
    assertEquals(1, history.findByOrder_IdOrderByChangedAtAscIdAsc(order.getId()).size());
  }

  @Test
  void confirmedPaymentCannotBeCancelled() {
    var customer = account(Role.ROLE_CUSTOMER); var order = order(customer); var product = product(10);
    item(order, product, 2);
    confirmations.confirmBankTransfer(order.getId(), account(Role.ROLE_STAFF).getId(), new PaymentConfirmationRequest());
    assertThrows(ConflictException.class, () -> cancellations.cancel(customer.getId(), order.getId(), request("Hủy")));
    assertEquals(OrderStatus.CONFIRMED, orders.findById(order.getId()).orElseThrow().getStatus());
    assertEquals(PaymentStatus.PAID, payments.findByOrder_Id(order.getId()).orElseThrow().getStatus());
    assertEquals(10, stock(product));
  }

  @Test
  void stoppedProductAndDuplicateProductLinesAreRestoredWithoutChangingCatalogData() {
    var customer = account(Role.ROLE_CUSTOMER); var order = order(customer); var product = product(10);
    item(order, product, 2); item(order, product, 3);
    product.setName("Bánh đã đổi tên"); product.setPrice(new BigDecimal("999000")); product.setStatus(ProductStatus.STOPPED);
    products.saveAndFlush(product);
    cancellations.cancel(customer.getId(), order.getId(), request("Hủy bánh"));
    var restored = products.findById(product.getId()).orElseThrow();
    assertEquals(15, restored.getStockQuantity()); assertEquals(ProductStatus.STOPPED, restored.getStatus());
    assertEquals("Bánh đã đổi tên", restored.getName());
    assertEquals(0, restored.getPrice().compareTo(new BigDecimal("999000")));
  }

  @Test
  void historyFailureRollsBackStockOrderAndCancellationFields() {
    var customer = account(Role.ROLE_CUSTOMER); var order = order(customer); var product = product(10);
    item(order, product, 2);
    doThrow(new IllegalStateException("simulated history failure")).when(history).saveAndFlush(any());
    try { assertThrows(IllegalStateException.class, () -> cancellations.cancel(customer.getId(), order.getId(), request("Hủy"))); }
    finally { reset(history); }
    assertPending(order); assertEquals(10, stock(product));
    var saved = orders.findById(order.getId()).orElseThrow();
    assertNull(saved.getCancellationReason()); assertNull(saved.getCancelledAt());
    assertEquals(order.getVersion(), saved.getVersion());
    assertEquals(product.getVersion(), products.findById(product.getId()).orElseThrow().getVersion());
  }

  @Test
  void laterStockOverflowRollsBackEarlierProductRestoration() {
    var customer = account(Role.ROLE_CUSTOMER); var order = order(customer);
    var first = product(10); var second = product(Integer.MAX_VALUE - 1);
    item(order, first, 2); item(order, second, 2);
    assertThrows(ConflictException.class, () -> cancellations.cancel(customer.getId(), order.getId(), request("Hủy")));
    assertPending(order); assertEquals(10, stock(first)); assertEquals(Integer.MAX_VALUE - 1, stock(second));
  }

  @Test
  void missingDetailsAndInvalidQuantityDoNotSilentlyCancel() {
    var customer = account(Role.ROLE_CUSTOMER); var empty = order(customer);
    assertThrows(ConflictException.class, () -> cancellations.cancel(customer.getId(), empty.getId(), request("Hủy")));
    assertPending(empty);
    var malformed = order(customer); var product = product(10); item(malformed, product, 0);
    assertThrows(ConflictException.class, () -> cancellations.cancel(customer.getId(), malformed.getId(), request("Hủy")));
    assertPending(malformed); assertEquals(10, stock(product));
    var missingPayment = order(customer); payments.delete(payments.findByOrder_Id(missingPayment.getId()).orElseThrow());
    assertThrows(ResourceNotFoundException.class,
        () -> cancellations.cancel(customer.getId(), missingPayment.getId(), request("Hủy")));
    assertEquals(OrderStatus.PENDING, orders.findById(missingPayment.getId()).orElseThrow().getStatus());
  }

  @Test
  void concurrentDuplicateCancellationOnlyRestoresOnce() throws Exception {
    var customer = account(Role.ROLE_CUSTOMER); var order = order(customer); var product = product(10);
    item(order, product, 2);
    Callable<Boolean> cancel = () -> cancelOrConflict(customer, order);
    assertEquals(1, concurrently(cancel, cancel));
    assertEquals(OrderStatus.CANCELLED, orders.findById(order.getId()).orElseThrow().getStatus());
    assertEquals(12, stock(product));
    assertEquals(1, history.findByOrder_IdOrderByChangedAtAscIdAsc(order.getId()).size());
  }

  @Test
  void paymentConfirmationAndCancellationCannotBothSucceed() throws Exception {
    var customer = account(Role.ROLE_CUSTOMER); var staff = account(Role.ROLE_STAFF);
    var order = order(customer); var product = product(10); item(order, product, 2);
    assertEquals(1, concurrently(() -> cancelOrConflict(customer, order), () -> {
      try { confirmations.confirmBankTransfer(order.getId(), staff.getId(), new PaymentConfirmationRequest()); return true; }
      catch (ConflictException expected) { return false; }
    }));
    var saved = orders.findById(order.getId()).orElseThrow();
    boolean cancelled = saved.getStatus() == OrderStatus.CANCELLED;
    assertEquals(cancelled ? PaymentStatus.PENDING : PaymentStatus.PAID,
        payments.findByOrder_Id(order.getId()).orElseThrow().getStatus());
    assertEquals(cancelled ? 12 : 10, stock(product));
    assertEquals(cancelled ? OrderStatus.CANCELLED : OrderStatus.CONFIRMED, saved.getStatus());
    assertEquals(1, history.findByOrder_IdOrderByChangedAtAscIdAsc(order.getId()).size());
  }

  @Test
  void differentOrdersSharingProductsRestoreAllStockWithoutLostUpdates() throws Exception {
    var firstCustomer = account(Role.ROLE_CUSTOMER); var secondCustomer = account(Role.ROLE_CUSTOMER);
    var firstOrder = order(firstCustomer); var secondOrder = order(secondCustomer);
    var first = product(10); var second = product(20);
    item(firstOrder, first, 2); item(firstOrder, second, 3);
    item(secondOrder, second, 4); item(secondOrder, first, 5);
    assertEquals(2, concurrently(() -> cancelOrConflict(firstCustomer, firstOrder),
        () -> cancelOrConflict(secondCustomer, secondOrder)));
    assertEquals(17, stock(first)); assertEquals(27, stock(second));
  }

  private boolean cancelOrConflict(Account customer, Order order) {
    try { cancellations.cancel(customer.getId(), order.getId(), request("Đổi kế hoạch")); return true; }
    catch (ConflictException expected) { return false; }
  }

  private int concurrently(Callable<Boolean> first, Callable<Boolean> second) throws Exception {
    var ready = new CountDownLatch(2); var start = new CountDownLatch(1); var pool = Executors.newFixedThreadPool(2);
    try {
      var jobs = List.of(first, second).stream().map(job -> pool.submit(() -> {
        ready.countDown(); assertTrue(start.await(10, TimeUnit.SECONDS)); return job.call();
      })).toList();
      assertTrue(ready.await(10, TimeUnit.SECONDS)); start.countDown();
      int successes = 0; for (var job : jobs) if (job.get(20, TimeUnit.SECONDS)) successes++;
      return successes;
    } finally { pool.shutdownNow(); }
  }

  private void assertPending(Order order) {
    assertEquals(OrderStatus.PENDING, orders.findById(order.getId()).orElseThrow().getStatus());
    assertEquals(PaymentStatus.PENDING, payments.findByOrder_Id(order.getId()).orElseThrow().getStatus());
    assertTrue(history.findByOrder_IdOrderByChangedAtAscIdAsc(order.getId()).isEmpty());
  }

  private int stock(Product product) { return products.findById(product.getId()).orElseThrow().getStockQuantity(); }
  private CancelOrderRequest request(String reason) { var request = new CancelOrderRequest(); request.setReason(reason); return request; }

  private Account account(Role role) {
    var account = new Account(); account.setEmail(UUID.randomUUID() + "@cancellation.test"); account.setPasswordHash("unused");
    account.setFullName("Khách kiểm thử hủy đơn"); account.setRole(role); account.setStatus(AccountStatus.ACTIVE);
    return accounts.saveAndFlush(account);
  }

  private Order order(Account customer) {
    var order = new Order(); order.setAccount(customer);
    order.setOrderCode("LN" + UUID.randomUUID().toString().replace("-", "").substring(0, 25).toUpperCase());
    order.setReceiverName(customer.getFullName()); order.setReceiverPhone("0912345678");
    order.setReceiverAddress("12 Nguyễn Huệ, TP. Hồ Chí Minh"); order.setDeliveryDate(LocalDate.of(2026, 11, 18));
    order.setTotalAmount(new BigDecimal("900000")); order.setStatus(OrderStatus.PENDING); orders.saveAndFlush(order);
    var payment = new Payment(); payment.setOrder(order); payment.setMethod(PaymentMethod.BANK_TRANSFER);
    payment.setStatus(PaymentStatus.PENDING); payment.setAmount(order.getTotalAmount()); payments.saveAndFlush(payment);
    return order;
  }

  private Product product(int stock) {
    var category = new Category(); category.setName("Cancellation " + UUID.randomUUID()); category.setActive(true);
    categories.saveAndFlush(category);
    var product = new Product(); product.setName("Bánh kem dâu"); product.setCategory(category);
    product.setDescription("Bánh cho kiểm thử hủy đơn"); product.setPrice(new BigDecimal("450000"));
    product.setStatus(ProductStatus.SELLING); product.setStockQuantity(stock); return products.saveAndFlush(product);
  }

  private void item(Order order, Product product, int quantity) {
    var detail = new OrderDetail(); detail.setOrder(order); detail.setProduct(product); detail.setProductName(product.getName());
    detail.setUnitPrice(product.getPrice()); detail.setQuantity(quantity); detail.setLineTotal(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
    details.saveAndFlush(detail);
  }

  private void preview(String name, MvcResult result) throws Exception {
    if (!Boolean.getBoolean("order.cancellationPreview")) return;
    var directory = Path.of("target", "cancellation-preview"); Files.createDirectories(directory);
    Files.writeString(directory.resolve(name + ".html"), result.getResponse().getContentAsString(StandardCharsets.UTF_8));
  }
}
