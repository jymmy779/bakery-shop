package com.bakery.cart.controller.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.bakery.cart.service.impl.*;
import com.bakery.catalog.dto.ProductDetailResponse;
import com.bakery.catalog.entity.ProductStatus;
import com.bakery.catalog.service.ProductQueryService;
import com.bakery.config.SecurityConfig;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringJUnitWebConfig(SessionCartIntegrationTest.Config.class)
class SessionCartIntegrationTest {
  @Configuration
  @EnableWebMvc
  @Import({SecurityConfig.class, CartQuantityController.class, CartQuantityServiceImpl.class,
      CartServiceImpl.class, SessionCartServiceImpl.class})
  static class Config {
    @Bean ProductQueryService products() { return mock(ProductQueryService.class); }
    @Bean UserDetailsService users() {
      return new InMemoryUserDetailsManager(User.withUsername("customer@example.test")
          .password(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
              .encode("test-password")).roles("CUSTOMER").build());
    }
  }

  @Autowired WebApplicationContext context;
  @Autowired ProductQueryService products;
  @Autowired SessionCartServiceImpl sessions;
  MockMvc mvc;

  @BeforeEach void setup() {
    reset(products);
    var product = new ProductDetailResponse();
    product.setId(1L);
    product.setName("Bánh kem");
    product.setPrice(new BigDecimal("120000"));
    product.setStockQuantity(5);
    product.setStatus(ProductStatus.SELLING);
    when(products.getById(1L)).thenReturn(product);
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  private MockHttpSession add() throws Exception {
    return (MockHttpSession) mvc.perform(post("/cart/items").with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content("{\"productId\":1,\"quantity\":2}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.totalAmount").value(240000))
        .andReturn().getRequest().getSession();
  }

  @Test void reloadRetainsCartAndOtherSessionIsIndependent() throws Exception {
    var session = add();
    mvc.perform(get("/cart/items").session(session)).andExpect(jsonPath("$.totalQuantity").value(2));
    mvc.perform(get("/cart/items")).andExpect(jsonPath("$.items").isEmpty())
        .andExpect(jsonPath("$.totalAmount").value(0));
    mvc.perform(get("/cart/items").session(session)).andExpect(jsonPath("$.totalQuantity").value(2));
  }

  @Test void addsSameItemUpdatesAndRemovesWithinSession() throws Exception {
    var session = add();
    mvc.perform(post("/cart/items").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
        .content("{\"productId\":1,\"quantity\":1}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.totalQuantity").value(3));
    mvc.perform(patch("/cart/items/1").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
        .content("{\"quantity\":1}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.totalAmount").value(120000));
    mvc.perform(delete("/cart/items/1").session(session).with(csrf()))
        .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty())
        .andExpect(jsonPath("$.totalAmount").value(0));
  }

  @Test void failedAddPreservesCartAndCsrfIsRequired() throws Exception {
    var session = add();
    mvc.perform(post("/cart/items").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
        .content("{\"productId\":1,\"quantity\":4}"))
        .andExpect(status().isConflict()).andExpect(jsonPath("$.message").exists());
    mvc.perform(delete("/cart/items/1").session(session)).andExpect(status().isForbidden());
    mvc.perform(get("/cart/items").session(session)).andExpect(jsonPath("$.totalQuantity").value(2));
  }

  @Test void customerLoginChangesSessionIdAndPreservesGuestCart() throws Exception {
    var session = add();
    String oldId = session.getId();
    mvc.perform(post("/login").session(session).with(csrf())
        .param("email", "customer@example.test").param("password", "test-password"))
        .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/"));
    assertNotEquals(oldId, session.getId());
    mvc.perform(get("/cart/items").session(session)).andExpect(jsonPath("$.totalQuantity").value(2));
  }

  @Test void newSessionAfterExpiryStartsEmpty() throws Exception {
    var session = add();
    session.invalidate(); // Container expiry invalidates the session in the same way.
    assertTrue(session.isInvalid());
    mvc.perform(get("/cart/items").session(new MockHttpSession()))
        .andExpect(jsonPath("$.items").isEmpty());
  }

  @Test void checkoutSuccessClearsOnlyCartAndFailurePreservesIt() throws Exception {
    var session = add();
    session.setAttribute("other", "keep");
    assertThrows(IllegalStateException.class, () -> sessions.checkout(session, cart -> {
      throw new IllegalStateException("checkout failed");
    }));
    assertNotNull(session.getAttribute("cart"));
    assertEquals("order-1", sessions.checkout(session, cart -> "order-1"));
    assertNull(session.getAttribute("cart"));
    assertEquals("keep", session.getAttribute("other"));
  }

  @Test void ambientTransactionClearsOnlyAfterCommitAndRetainsOnRollback() throws Exception {
    var session = add();
    TransactionSynchronizationManager.setActualTransactionActive(true);
    TransactionSynchronizationManager.initSynchronization();
    try {
      sessions.checkout(session, cart -> "order-1");
      assertNotNull(session.getAttribute("cart"));
      var callbacks = TransactionSynchronizationManager.getSynchronizations();
      callbacks.forEach(callback -> callback.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
      assertNotNull(session.getAttribute("cart"));
      TransactionSynchronizationManager.clearSynchronization();
      TransactionSynchronizationManager.initSynchronization();
      sessions.checkout(session, cart -> "order-2");
      TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
      assertNull(session.getAttribute("cart"));
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
      TransactionSynchronizationManager.setActualTransactionActive(false);
    }
  }
}
