package com.bakery.catalog.service;

import com.bakery.account.entity.Account;
import com.bakery.account.entity.AccountStatus;
import com.bakery.account.entity.Role;
import com.bakery.catalog.entity.Category;
import com.bakery.catalog.entity.Product;
import com.bakery.catalog.entity.ProductStatus;
import com.bakery.order.entity.Order;
import com.bakery.order.entity.OrderDetail;
import com.bakery.order.entity.OrderStatus;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"app.bootstrap-admin.enabled=false", "spring.jpa.hibernate.ddl-auto=none",
    "spring.jpa.show-sql=false", "logging.level.root=WARN", "logging.level.com.bakery=WARN"})
@Transactional
class ProductFilterIntegrationTest {
  @Autowired EntityManager em;
  @Autowired ProductQueryService productQueryService;

  @Test
  void bestSellingCountsOnlyDeliveredQuantitiesAndCombinesFilters() {
    String marker = UUID.randomUUID().toString();
    Category category = new Category();
    category.setName(marker);
    category.setActive(true);
    em.persist(category);
    Account account = new Account();
    account.setEmail(marker + "@example.test");
    account.setPasswordHash("unused-test-password-hash");
    account.setFullName("Filter test");
    account.setRole(Role.ROLE_CUSTOMER);
    account.setStatus(AccountStatus.ACTIVE);
    em.persist(account);
    Product unsold = product(marker + " A", category, "100000");
    Product sold = product(marker + " Z", category, "200000");
    order(account, sold, OrderStatus.DELIVERED, 3);
    order(account, sold, OrderStatus.DELIVERED, 4);
    order(account, unsold, OrderStatus.CANCELLED, 100);
    order(account, unsold, OrderStatus.PENDING, 200);
    em.flush();

    var first = productQueryService.filter(marker, category.getId(), new BigDecimal("100000"),
        new BigDecimal("200000"), "best_selling", 0, 1);
    assertEquals(2, first.getTotalElements());
    assertEquals(sold.getId(), first.getContent().get(0).getId());
    var second = productQueryService.filter(marker, category.getId(), new BigDecimal("100000"),
        new BigDecimal("200000"), "best_selling", 1, 1);
    assertEquals(unsold.getId(), second.getContent().get(0).getId());
    assertEquals(unsold.getId(), productQueryService.filter(marker, category.getId(), null, null,
        "price_asc", 0, 1).getContent().get(0).getId());
  }

  private Product product(String name, Category category, String price) {
    Product product = new Product();
    product.setName(name);
    product.setDescription("Filter integration test");
    product.setPrice(new BigDecimal(price));
    product.setStatus(ProductStatus.SELLING);
    product.setCategory(category);
    em.persist(product);
    return product;
  }

  private void order(Account account, Product product, OrderStatus status, int quantity) {
    Order order = new Order();
    order.setOrderCode(UUID.randomUUID().toString().substring(0, 25));
    order.setAccount(account);
    order.setReceiverName("Filter test");
    order.setReceiverPhone("0900000000");
    order.setReceiverAddress("Test address");
    order.setDeliveryDate(LocalDate.of(2026, 10, 19));
    order.setTotalAmount(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
    order.setStatus(status);
    em.persist(order);
    OrderDetail detail = new OrderDetail();
    detail.setOrder(order);
    detail.setProduct(product);
    detail.setProductName(product.getName());
    detail.setUnitPrice(product.getPrice());
    detail.setQuantity(quantity);
    detail.setLineTotal(order.getTotalAmount());
    em.persist(detail);
  }
}
