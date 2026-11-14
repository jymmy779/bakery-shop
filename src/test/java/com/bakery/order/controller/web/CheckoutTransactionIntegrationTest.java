package com.bakery.order.controller.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

import com.bakery.account.entity.Account;
import com.bakery.account.entity.AccountStatus;
import com.bakery.account.entity.Role;
import com.bakery.account.repository.AccountRepository;
import com.bakery.account.security.CustomUserDetails;
import com.bakery.cart.entity.AccountCart;
import com.bakery.cart.entity.AccountCartItem;
import com.bakery.cart.repository.AccountCartRepository;
import com.bakery.catalog.entity.Category;
import com.bakery.catalog.entity.Product;
import com.bakery.catalog.entity.ProductStatus;
import com.bakery.catalog.repository.CategoryRepository;
import com.bakery.catalog.repository.ProductRepository;
import com.bakery.order.dto.CheckoutRequest;
import com.bakery.order.entity.OrderStatus;
import com.bakery.order.entity.PaymentStatus;
import com.bakery.order.repository.OrderRepository;
import com.bakery.order.repository.OrderDetailRepository;
import com.bakery.order.repository.PaymentRepository;
import com.bakery.order.repository.OrderStatusHistoryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Opt-in isolated DB checks; verification adds H2 to a temporary build, never to the app POM. */
@EnabledIfSystemProperty(named = "checkout.transactionTests", matches = "true")
@ActiveProfiles("checkout-test")
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:checkout;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
    "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.open-in-view=false",
    "spring.sql.init.mode=never", "app.bootstrap-admin.enabled=false", "logging.level.root=WARN"
})
@AutoConfigureMockMvc
class CheckoutTransactionIntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired AccountRepository accounts;
  @Autowired AccountCartRepository carts;
  @Autowired CategoryRepository categories;
  @Autowired ProductRepository products;
  @Autowired OrderRepository orders;
  @Autowired OrderDetailRepository details;
  @Autowired PaymentRepository payments;
  @MockitoSpyBean OrderStatusHistoryRepository history;

  @Test
  void placesOrderClearsCartAndReplayingPostReturnsSameOrder() throws Exception {
    var fixture = fixture(null, 5, 2);
    String token = token(fixture);
    long initialOrders = orders.count();
    var first = mvc.perform(place(fixture, token).param("totalAmount", "1"))
        .andExpect(status().is3xxRedirection()).andReturn();
    var order = orders.findByOrderCodeAndAccount_Id(
        com.bakery.order.service.impl.CheckoutQuoteStore.orderCode(token), fixture.account().getId()).orElseThrow();
    assertEquals(OrderStatus.PENDING, order.getStatus());
    assertEquals(0, order.getTotalAmount().compareTo(new BigDecimal("900000")));
    assertEquals(PaymentStatus.PENDING, payments.findByOrder_Id(order.getId()).orElseThrow().getStatus());
    assertEquals("Ít ngọt", details.findByOrder_IdOrderByIdAsc(order.getId()).get(0).getNote());
    assertEquals(3, products.findById(fixture.product().getId()).orElseThrow().getStockQuantity());
    mvc.perform(get("/cart/items").session(fixture.session()).with(user(fixture.principal())))
        .andExpect(jsonPath("$.items.length()").value(0));
    mvc.perform(place(fixture, token)).andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(first.getResponse().getRedirectedUrl()));
    assertEquals(initialOrders + 1, orders.count());
    var receipt = mvc.perform(get(first.getResponse().getRedirectedUrl()).with(user(fixture.principal())))
        .andExpect(status().isOk()).andExpect(content().string(containsString(order.getOrderCode())))
        .andExpect(content().string(containsString("900.000 ₫"))).andReturn();
    if (Boolean.getBoolean("checkout.preview")) {
      var directory = java.nio.file.Path.of("target", "checkout-preview");
      java.nio.file.Files.createDirectories(directory);
      java.nio.file.Files.writeString(directory.resolve("checkout-success.html"),
          receipt.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }
  }

  @Test
  void failureAfterSavingPaymentRollsBackStockOrderDetailsPaymentAndCart() throws Exception {
    var fixture = fixture(null, 5, 2);
    String token = token(fixture);
    long initialOrders = orders.count(), initialDetails = details.count(), initialPayments = payments.count();
    doThrow(new DataIntegrityViolationException("simulated final history failure")).when(history).saveAndFlush(any());
    mvc.perform(place(fixture, token)).andExpect(status().isOk())
        .andExpect(content().string(containsString("Chưa thể hoàn tất đặt hàng")));
    assertEquals(initialOrders, orders.count());
    assertEquals(initialDetails, details.count());
    assertEquals(initialPayments, payments.count());
    assertEquals(5, products.findById(fixture.product().getId()).orElseThrow().getStockQuantity());
    mvc.perform(get("/cart/items").with(user(fixture.principal())))
        .andExpect(jsonPath("$.totalQuantity").value(2));
  }

  @Test
  void changedPriceRequiresCustomerToReviewAgain() throws Exception {
    var fixture = fixture(null, 5, 1);
    String token = token(fixture);
    var product = products.findById(fixture.product().getId()).orElseThrow();
    product.setPrice(new BigDecimal("500000"));
    products.saveAndFlush(product);
    long initialOrders = orders.count();
    mvc.perform(place(fixture, token)).andExpect(status().isOk())
        .andExpect(content().string(containsString("Giá hoặc số lượng bánh đã thay đổi")))
        .andExpect(content().string(containsString("500.000 ₫")))
        .andExpect(content().string(containsString("Người nhận kiểm thử")));
    assertEquals(initialOrders, orders.count());
    assertEquals(5, products.findById(product.getId()).orElseThrow().getStockQuantity());
  }

  @Test
  void preparationDateFailureAlsoRollsBackTheStockDeduction() throws Exception {
    var fixture = fixture(null, 5, 1);
    String token = token(fixture);
    long initialOrders = orders.count();
    mvc.perform(place(fixture, token).with(request -> {
      request.setParameter("deliveryDate", today().toString());
      return request;
    }))
        .andExpect(status().isOk()).andExpect(content().string(containsString("Ngày nhận bánh sớm nhất")));
    assertEquals(initialOrders, orders.count());
    assertEquals(5, products.findById(fixture.product().getId()).orElseThrow().getStockQuantity());
  }

  @Test
  void concurrentCustomersCannotBuyMoreThanTheLastAvailableCake() throws Exception {
    var first = fixture(null, 1, 1);
    var second = fixture(first.product(), 1, 1);
    long initialOrders = orders.count();
    var statuses = race(first, token(first), second, token(second));
    assertEquals(1, statuses.stream().filter(code -> code == 302).count());
    assertEquals(1, statuses.stream().filter(code -> code == 200).count());
    assertEquals(initialOrders + 1, orders.count());
    assertEquals(0, products.findById(first.product().getId()).orElseThrow().getStockQuantity());
  }

  @Test
  void twoSessionsCannotPlaceTheSameAccountCartTwice() throws Exception {
    var first = fixture(null, 5, 2);
    var second = new Fixture(first.account(), first.product(), new MockHttpSession(), first.principal());
    long initialOrders = orders.count();
    var statuses = race(first, token(first), second, token(second));
    assertEquals(1, statuses.stream().filter(code -> code == 302).count());
    assertEquals(initialOrders + 1, orders.count());
    assertEquals(3, products.findById(first.product().getId()).orElseThrow().getStockQuantity());
  }

  @Test
  void anotherCustomerCannotReadTheResult() throws Exception {
    var owner = fixture(null, 5, 1);
    String location = mvc.perform(place(owner, token(owner)))
        .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
    var stranger = fixture(null, 5, 1);
    mvc.perform(get(location).with(user(stranger.principal()))).andExpect(status().isNotFound());
  }

  @Test
  void stoppedProductAfterReviewCannotBePurchased() throws Exception {
    var fixture = fixture(null, 5, 1);
    String token = token(fixture);
    var product = products.findById(fixture.product().getId()).orElseThrow();
    product.setStatus(ProductStatus.STOPPED);
    products.saveAndFlush(product);
    long initialOrders = orders.count();
    mvc.perform(place(fixture, token)).andExpect(status().isOk())
        .andExpect(content().string(containsString("Sản phẩm đã ngừng bán")));
    assertEquals(initialOrders, orders.count());
    assertEquals(5, products.findById(product.getId()).orElseThrow().getStockQuantity());
  }

  @Test
  void notesCannotReferToProductsOutsideTheReviewedCart() throws Exception {
    var fixture = fixture(null, 5, 1);
    String token = token(fixture);
    long initialOrders = orders.count();
    mvc.perform(place(fixture, token).param("productNotes[999999]", "Bánh ngoài giỏ"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Lời nhắn chứa bánh không thuộc giỏ hàng")));
    assertEquals(initialOrders, orders.count());
    assertEquals(5, products.findById(fixture.product().getId()).orElseThrow().getStockQuantity());
  }

  private java.util.List<Integer> race(Fixture first, String firstToken, Fixture second, String secondToken)
      throws Exception {
    var pool = Executors.newFixedThreadPool(2);
    try {
      var start = new CountDownLatch(1);
      var a = pool.submit(() -> { start.await(); return mvc.perform(place(first, firstToken)).andReturn().getResponse().getStatus(); });
      var b = pool.submit(() -> { start.await(); return mvc.perform(place(second, secondToken)).andReturn().getResponse().getStatus(); });
      start.countDown();
      return java.util.List.of(a.get(25, TimeUnit.SECONDS), b.get(25, TimeUnit.SECONDS));
    } finally {
      pool.shutdownNow();
    }
  }

  private String token(Fixture fixture) throws Exception {
    var response = mvc.perform(get("/checkout").session(fixture.session()).with(user(fixture.principal())))
        .andExpect(status().isOk()).andReturn();
    return ((CheckoutRequest) response.getModelAndView().getModel().get("checkoutRequest")).getCheckoutToken();
  }

  private MockHttpServletRequestBuilder place(Fixture fixture, String token) {
    return post("/checkout").session(fixture.session()).with(user(fixture.principal())).with(csrf())
        .param("checkoutToken", token).param("receiverName", "Người nhận kiểm thử")
        .param("receiverPhone", "0912345678").param("receiverAddress", "Địa chỉ nhận bánh kiểm thử")
        .param("receiverEmail", "").param("deliveryDate", today().plusDays(3).toString())
        .param("productNotes[" + fixture.product().getId() + "]", "Ít ngọt");
  }

  private Fixture fixture(Product shared, int stock, int quantity) {
    var account = new Account();
    account.setEmail("checkout-" + UUID.randomUUID() + "@example.com");
    account.setPasswordHash("test-only");
    account.setFullName("Khách kiểm thử");
    account.setPhone("0912345678");
    account.setRole(Role.ROLE_CUSTOMER);
    account.setStatus(AccountStatus.ACTIVE);
    accounts.saveAndFlush(account);
    Product product = shared;
    if (product == null) {
      var category = new Category();
      category.setName("Kiểm thử " + UUID.randomUUID());
      category.setActive(true);
      categories.saveAndFlush(category);
      product = new Product();
      product.setName("Bánh kem kiểm thử");
      product.setDescription("Dữ liệu database kiểm thử riêng");
      product.setCategory(category);
      product.setPrice(new BigDecimal("450000"));
      product.setStockQuantity(stock);
      product.setPreparationDays(1);
      product.setStatus(ProductStatus.SELLING);
      products.saveAndFlush(product);
    }
    var cart = new AccountCart();
    cart.setAccount(account);
    var item = new AccountCartItem();
    item.setCart(cart);
    item.setProduct(product);
    item.setQuantity(quantity);
    cart.getItems().add(item);
    carts.saveAndFlush(cart);
    return new Fixture(account, product, new MockHttpSession(), new CustomUserDetails(account));
  }

  private LocalDate today() { return LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")); }
  private record Fixture(Account account, Product product, MockHttpSession session, CustomUserDetails principal) {}
}
