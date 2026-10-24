package com.bakery.cart.controller.web;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.bakery.cart.service.impl.CartQuantityServiceImpl;
import com.bakery.cart.service.impl.CartServiceImpl;
import com.bakery.cart.service.impl.SessionCartServiceImpl;
import com.bakery.catalog.service.ProductQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CartQuantityControllerTest {
  private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new CartQuantityController(
      new SessionCartServiceImpl(new CartServiceImpl(mock(ProductQueryService.class),
          new CartQuantityServiceImpl(mock(ProductQueryService.class)))))).build();

  @Test
  void absentCartReturnsJson404() throws Exception {
    mvc.perform(patch("/cart/items/1").contentType(MediaType.APPLICATION_JSON)
        .content("{\"quantity\":2}"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  void invalidBodyReturnsJson400() throws Exception {
    for (String body : new String[]{"{}", "{\"quantity\":0}", "{\"quantity\":-1}", "invalid"}) {
      mvc.perform(patch("/cart/items/1").contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").exists());
    }
  }
}
