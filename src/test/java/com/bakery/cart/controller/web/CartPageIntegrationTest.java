package com.bakery.cart.controller.web;

import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.bakery.cart.service.impl.CartQuantityServiceImpl;
import com.bakery.cart.service.impl.CartServiceImpl;
import com.bakery.cart.service.impl.SessionCartServiceImpl;
import com.bakery.catalog.dto.ProductDetailResponse;
import com.bakery.catalog.entity.ProductStatus;
import com.bakery.catalog.service.ProductQueryService;
import com.bakery.config.SecurityConfig;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({CartController.class, CartQuantityController.class})
@Import({SecurityConfig.class, CartQuantityServiceImpl.class, CartServiceImpl.class, SessionCartServiceImpl.class})
class CartPageIntegrationTest {
  @Autowired MockMvc mvc;
  @MockitoBean ProductQueryService products;
  @MockitoBean com.bakery.account.repository.AccountRepository accounts;
  ProductDetailResponse cake;

  @BeforeEach void setup() {
    cake = new ProductDetailResponse();
    cake.setId(1L);
    cake.setName("Bánh kem dâu từ Session");
    cake.setPrice(new BigDecimal("250000"));
    cake.setStockQuantity(3);
    cake.setStatus(ProductStatus.SELLING);
    when(products.getById(1L)).thenReturn(cake);
  }

  MockHttpSession addCake() throws Exception {
    return (MockHttpSession) mvc.perform(post("/cart/items").with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content("{\"productId\":1,\"quantity\":1}"))
        .andExpect(status().isOk()).andReturn().getRequest().getSession();
  }

  @Test void addThenOpenCartUpdateQuantityAndRemoveLastItem() throws Exception {
    var session = addCake();
    mvc.perform(get("/cart").session(session)).andExpect(status().isOk())
        .andExpect(content().string(containsString("Bánh kem dâu từ Session")))
        .andExpect(content().string(containsString("250.000")))
        .andExpect(content().string(containsString("/js/cart.js")))
        .andExpect(content().string(containsString("</html>")));
    mvc.perform(patch("/cart/items/1").session(session).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":2}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.totalAmount").value(500000));
    mvc.perform(get("/cart").session(session)).andExpect(status().isOk())
        .andExpect(content().string(containsString("500.000")));
    mvc.perform(patch("/cart/items/1").session(session).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":1}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.totalQuantity").value(1));
    mvc.perform(delete("/cart/items/1").session(session).with(csrf())).andExpect(status().isOk());
    mvc.perform(get("/cart").session(session)).andExpect(status().isOk())
        .andExpect(content().string(containsString("Giỏ hàng đang trống.")));
  }

  @Test void stoppedItemStaysVisibleAndRemovableWithoutCheckoutLink() throws Exception {
    var session = addCake();
    cake.setStatus(ProductStatus.STOPPED);
    mvc.perform(get("/cart").session(session)).andExpect(status().isOk())
        .andExpect(content().string(containsString("Sản phẩm đã ngừng bán")))
        .andExpect(content().string(containsString("data-action=\"remove\"")))
        .andExpect(content().string(not(containsString("href=\"/checkout\""))));
    mvc.perform(delete("/cart/items/1").session(session).with(csrf())).andExpect(status().isOk());
  }

  @Test void rejectsExcessStockAndCartIsIndependentBetweenSessions() throws Exception {
    var session = addCake();
    mvc.perform(patch("/cart/items/1").session(session).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":4}"))
        .andExpect(status().isConflict());
    mvc.perform(get("/cart")).andExpect(status().isOk())
        .andExpect(content().string(containsString("Giỏ hàng đang trống.")));
    mvc.perform(get("/cart/items").session(session)).andExpect(jsonPath("$.totalQuantity").value(1));
  }
}
