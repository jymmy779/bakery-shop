package com.bakery.order.controller.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.bakery.order.dto.OrderDetailResponse;
import com.bakery.order.dto.OrderSummaryResponse;
import com.bakery.order.entity.Order;
import com.bakery.order.entity.OrderDetail;
import com.bakery.order.entity.OrderStatus;
import com.bakery.order.entity.OrderStatusHistory;
import com.bakery.order.entity.Payment;
import com.bakery.order.entity.PaymentMethod;
import com.bakery.order.entity.PaymentStatus;
import com.bakery.order.repository.OrderDetailRepository;
import com.bakery.order.repository.OrderRepository;
import com.bakery.order.repository.OrderStatusHistoryRepository;
import com.bakery.order.repository.PaymentRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Opt-in isolated database checks, with H2 supplied by the temporary verification build. */
@EnabledIfSystemProperty(named = "order.queryTests", matches = "true")
@ActiveProfiles("order-query-test")
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:customerOrders;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
    "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.open-in-view=false",
    "spring.sql.init.mode=never", "app.bootstrap-admin.enabled=false", "logging.level.root=WARN"
})
@AutoConfigureMockMvc
class CustomerOrderIntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired AccountRepository accounts;
  @Autowired OrderRepository orders;
  @Autowired PaymentRepository payments;
  @Autowired OrderDetailRepository details;
  @Autowired OrderStatusHistoryRepository history;
  @Autowired CategoryRepository categories;
  @Autowired ProductRepository products;
  @Autowired JdbcTemplate jdbc;

  @Test
  void listIsScopedToAuthenticatedCustomerEvenWithForgedAccountParameter() throws Exception {
    var customer = account(Role.ROLE_CUSTOMER);
    var other = account(Role.ROLE_CUSTOMER);
    var mine = order(customer, OrderStatus.PENDING);
    var foreign = order(other, OrderStatus.DELIVERED);
    var result = mvc.perform(get("/orders").param("accountId", other.getId().toString())
            .with(user(new CustomUserDetails(customer))))
        .andExpect(status().isOk()).andExpect(view().name("order/list"))
        .andExpect(content().string(containsString(mine.getOrderCode())))
        .andExpect(content().string(not(containsString(foreign.getOrderCode())))).andReturn();
    assertEquals(1, page(result).getTotalElements());
  }

  @Test
  void paginatesNewestFirstWithStableTieBreakingAndNoDuplicates() throws Exception {
    var customer = account(Role.ROLE_CUSTOMER);
    var created = new ArrayList<Long>();
    for (int index = 0; index < 8; index++) {
      var order = order(customer, OrderStatus.PENDING);
      created.add(order.getId());
      jdbc.update("UPDATE orders SET created_at = ? WHERE id = ?",
          LocalDateTime.of(2026, 11, 14, 9, 0), order.getId());
    }
    var first = mvc.perform(get("/orders").with(user(new CustomUserDetails(customer))))
        .andExpect(status().isOk()).andReturn();
    var second = mvc.perform(get("/orders?page=1").with(user(new CustomUserDetails(customer))))
        .andExpect(status().isOk()).andReturn();
    assertEquals(8, page(first).getTotalElements());
    assertEquals(2, page(first).getTotalPages());
    assertEquals(List.of(created.get(7), created.get(6), created.get(5), created.get(4), created.get(3), created.get(2)),
        page(first).getContent().stream().map(OrderSummaryResponse::getId).toList());
    assertEquals(List.of(created.get(1), created.get(0)),
        page(second).getContent().stream().map(OrderSummaryResponse::getId).toList());
    savePreview("list", first);
  }

  @Test
  void filtersBeforePaginationAndKeepsStatusInPageLinks() throws Exception {
    var customer = account(Role.ROLE_CUSTOMER);
    for (int index = 0; index < 7; index++) order(customer, OrderStatus.CONFIRMED);
    order(customer, OrderStatus.CANCELLED);
    order(account(Role.ROLE_CUSTOMER), OrderStatus.CONFIRMED);
    var result = mvc.perform(get("/orders?status=CONFIRMED").with(user(new CustomUserDetails(customer))))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("page=1&amp;status=CONFIRMED"))).andReturn();
    assertEquals(7, page(result).getTotalElements());
    assertTrue(page(result).stream().allMatch(order -> order.getStatus() == OrderStatus.CONFIRMED));
    assertEquals(OrderStatus.CONFIRMED, result.getModelAndView().getModel().get("selectedStatus"));
    savePreview("filtered", result);
  }

  @Test
  void normalizesNegativeAndOutOfRangePageIndexes() throws Exception {
    var customer = account(Role.ROLE_CUSTOMER);
    order(customer, OrderStatus.PENDING);
    for (String index : List.of("-1", "999")) {
      var result = mvc.perform(get("/orders").param("page", index).with(user(new CustomUserDetails(customer))))
          .andExpect(status().isOk()).andReturn();
      assertEquals(0, page(result).getNumber());
      assertEquals(1, page(result).getNumberOfElements());
    }
  }

  @Test
  void emptyAccountAndEmptyFilterHaveUsefulDifferentMessages() throws Exception {
    var customer = account(Role.ROLE_CUSTOMER);
    var empty = mvc.perform(get("/orders?page=999").with(user(new CustomUserDetails(customer))))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Chưa có đơn bánh nào ở đây."))).andReturn();
    assertEquals(0, page(empty).getNumber());
    savePreview("empty", empty);
    order(customer, OrderStatus.PENDING);
    var filtered = mvc.perform(get("/orders?status=DELIVERED").with(user(new CustomUserDetails(customer))))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Chưa có đơn ở trạng thái này."))).andReturn();
    assertEquals(0, page(filtered).getTotalElements());
    savePreview("empty-filter", filtered);
  }

  @Test
  void detailReadsSnapshotsEvenAfterProductIsRenamedRepricedAndStopped() throws Exception {
    var customer = account(Role.ROLE_CUSTOMER);
    var order = order(customer, OrderStatus.CONFIRMED);
    var product = item(order, "Bánh kem dâu <đặc biệt>", "Ít ngọt, viết chữ Chúc mừng");
    product.setName("Tên bánh đã đổi");
    product.setPrice(new BigDecimal("999000"));
    product.setStatus(ProductStatus.STOPPED);
    products.saveAndFlush(product);
    var at = LocalDateTime.of(2026, 11, 14, 9, 0);
    event(order, null, OrderStatus.PENDING, at, customer, "Khách hàng đặt bánh");
    event(order, OrderStatus.PENDING, OrderStatus.CONFIRMED, at.plusHours(1), null, "Bếp đã nhận đơn");
    var result = mvc.perform(get("/orders/{id}", order.getId()).with(user(new CustomUserDetails(customer))))
        .andExpect(status().isOk()).andExpect(view().name("order/detail"))
        .andExpect(content().string(containsString("Bánh kem dâu &lt;đặc biệt&gt;")))
        .andExpect(content().string(not(containsString("Tên bánh đã đổi"))))
        .andExpect(content().string(containsString("450.000 ₫")))
        .andExpect(content().string(containsString("900.000 ₫")))
        .andExpect(content().string(containsString("Hành trình đơn bánh"))).andReturn();
    var detail = detail(result);
    assertEquals(2, detail.getItems().get(0).getQuantity());
    assertEquals("Ít ngọt, viết chữ Chúc mừng", detail.getItems().get(0).getNote());
    assertEquals(customer.getFullName(), detail.getStatusHistory().get(0).getChangedByName());
    assertNull(detail.getStatusHistory().get(1).getChangedByName());
    savePreview("detail", result);
  }

  @Test
  void timelineIsChronologicalWithStableOrderWhenTimestampsMatch() throws Exception {
    var customer = account(Role.ROLE_CUSTOMER);
    var order = order(customer, OrderStatus.DELIVERED);
    var at = LocalDateTime.of(2026, 11, 14, 9, 0);
    event(order, OrderStatus.CONFIRMED, OrderStatus.DELIVERING, at.plusHours(1), null, "Giao bánh");
    event(order, null, OrderStatus.PENDING, at, customer, "Đặt bánh");
    event(order, OrderStatus.PENDING, OrderStatus.CONFIRMED, at, null, "Nhận đơn");
    var result = mvc.perform(get("/orders/{id}", order.getId()).with(user(new CustomUserDetails(customer))))
        .andExpect(status().isOk()).andReturn();
    assertEquals(List.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.DELIVERING),
        detail(result).getStatusHistory().stream().map(entry -> entry.getToStatus()).toList());
  }

  @Test
  void showsPaidPaymentAndRecordedCancellationWithoutMutationControls() throws Exception {
    var customer = account(Role.ROLE_CUSTOMER);
    var order = order(customer, OrderStatus.CANCELLED);
    item(order, "Bánh hạnh nhân", null);
    order.setCancellationReason("Khách đổi kế hoạch");
    order.setCancelledAt(LocalDateTime.of(2026, 11, 14, 10, 30));
    orders.saveAndFlush(order);
    var payment = payments.findByOrder_Id(order.getId()).orElseThrow();
    payment.setStatus(PaymentStatus.PAID);
    payment.setPaidAt(LocalDateTime.of(2026, 11, 14, 10, 0));
    payment.setBankReference("BANK-123");
    payments.saveAndFlush(payment);
    var result = mvc.perform(get("/orders/{id}", order.getId()).with(user(new CustomUserDetails(customer))))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Đã thanh toán")))
        .andExpect(content().string(containsString("Khách đổi kế hoạch")))
        .andExpect(content().string(containsString("BANK-123")))
        .andExpect(content().string(containsString("Chưa có lịch sử cập nhật")))
        .andExpect(content().string(not(containsString("method=\"post\"")))).andReturn();
    assertEquals(order.getCancelledAt(), detail(result).getCancelledAt());
    savePreview("cancelled", result);
  }

  @Test
  void customerCannotReadForeignOrNonexistentOrder() throws Exception {
    var customer = account(Role.ROLE_CUSTOMER);
    var foreign = order(account(Role.ROLE_CUSTOMER), OrderStatus.PENDING);
    for (Long id : List.of(foreign.getId(), Long.MAX_VALUE)) {
      mvc.perform(get("/orders/{id}", id).with(user(new CustomUserDetails(customer))))
          .andExpect(status().isNotFound()).andExpect(content().string(not(containsString(foreign.getOrderCode()))));
    }
  }

  @Test
  void guestMustLogInAndStaffAdminCannotUseCustomerPages() throws Exception {
    for (String path : List.of("/orders", "/orders/1")) {
      mvc.perform(get(path)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
      for (Role role : List.of(Role.ROLE_STAFF, Role.ROLE_ADMIN)) {
        mvc.perform(get(path).with(user(new CustomUserDetails(account(role)))))
            .andExpect(status().isForbidden());
      }
    }
  }

  @Test
  void escapesStoredRecipientNotesAndHistoryText() throws Exception {
    var customer = account(Role.ROLE_CUSTOMER);
    var order = order(customer, OrderStatus.PENDING);
    order.setReceiverName("<script>alert('recipient')</script>");
    order.setNote("<img src=x onerror=alert(1)>");
    orders.saveAndFlush(order);
    item(order, "Bánh <b>kem</b>", "<script>alert('note')</script>");
    event(order, null, OrderStatus.PENDING, LocalDateTime.now(), null, "<script>alert('history')</script>");
    mvc.perform(get("/orders").with(user(new CustomUserDetails(customer))))
        .andExpect(status().isOk()).andExpect(content().string(containsString("&lt;script&gt;")))
        .andExpect(content().string(not(containsString("<script>alert"))));
    mvc.perform(get("/orders/{id}", order.getId()).with(user(new CustomUserDetails(customer))))
        .andExpect(status().isOk()).andExpect(content().string(containsString("&lt;img")))
        .andExpect(content().string(not(containsString("<script>alert"))))
        .andExpect(content().string(not(containsString("<img src=x"))));
  }

  @Test
  void rejectsInvalidFiltersAndPageNumbers() throws Exception {
    var customer = account(Role.ROLE_CUSTOMER);
    for (String query : List.of("status=UNKNOWN", "page=abc", "page=999999999999999999")) {
      mvc.perform(get("/orders?" + query).with(user(new CustomUserDetails(customer))))
          .andExpect(status().isBadRequest());
    }
  }

  private Account account(Role role) {
    var account = new Account();
    account.setEmail(UUID.randomUUID() + "@orders.test");
    account.setPasswordHash("unused");
    account.setFullName("Phạm Nhật Hưng");
    account.setRole(role);
    account.setStatus(AccountStatus.ACTIVE);
    return accounts.saveAndFlush(account);
  }

  private Order order(Account account, OrderStatus status) {
    var order = new Order();
    order.setAccount(account);
    order.setOrderCode("LN" + UUID.randomUUID().toString().replace("-", "").substring(0, 25).toUpperCase());
    order.setReceiverName("Phạm Nhật Hưng");
    order.setReceiverPhone("0912345678");
    order.setReceiverAddress("12 Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh");
    order.setReceiverEmail("hung@orders.test");
    order.setDeliveryDate(LocalDate.of(2026, 11, 18));
    order.setDeliveryTime(LocalTime.of(15, 30));
    order.setNote("Gọi trước khi giao. Cảm ơn LUNE!");
    order.setTotalAmount(new BigDecimal("900000"));
    order.setStatus(status);
    order = orders.saveAndFlush(order);
    var payment = new Payment();
    payment.setOrder(order);
    payment.setMethod(PaymentMethod.BANK_TRANSFER);
    payment.setStatus(PaymentStatus.PENDING);
    payment.setAmount(order.getTotalAmount());
    payments.saveAndFlush(payment);
    return order;
  }

  private Product item(Order order, String snapshotName, String note) {
    var category = new Category();
    category.setName("Bánh " + UUID.randomUUID());
    category.setActive(true);
    categories.saveAndFlush(category);
    var product = new Product();
    product.setName("Bánh hiện tại");
    product.setDescription("Bánh cho kiểm thử đơn hàng");
    product.setCategory(category);
    product.setPrice(new BigDecimal("450000"));
    product.setStatus(ProductStatus.SELLING);
    product.setStockQuantity(10);
    product = products.saveAndFlush(product);
    var detail = new OrderDetail();
    detail.setOrder(order);
    detail.setProduct(product);
    detail.setProductName(snapshotName);
    detail.setUnitPrice(new BigDecimal("450000"));
    detail.setQuantity(2);
    detail.setLineTotal(new BigDecimal("900000"));
    detail.setNote(note);
    details.saveAndFlush(detail);
    return product;
  }

  private void event(Order order, OrderStatus from, OrderStatus to, LocalDateTime at, Account actor, String note) {
    var event = new OrderStatusHistory();
    event.setOrder(order);
    event.setFromStatus(from);
    event.setToStatus(to);
    event.setChangedAt(at);
    event.setChangedBy(actor);
    event.setNote(note);
    history.saveAndFlush(event);
  }

  @SuppressWarnings("unchecked")
  private Page<OrderSummaryResponse> page(MvcResult result) {
    return (Page<OrderSummaryResponse>) result.getModelAndView().getModel().get("orders");
  }

  private OrderDetailResponse detail(MvcResult result) {
    return (OrderDetailResponse) result.getModelAndView().getModel().get("order");
  }

  private void savePreview(String name, MvcResult result) throws Exception {
    if (!Boolean.getBoolean("order.preview")) return;
    var directory = Path.of("target", "order-preview");
    Files.createDirectories(directory);
    Files.writeString(directory.resolve(name + ".html"), result.getResponse().getContentAsString(StandardCharsets.UTF_8));
  }
}
