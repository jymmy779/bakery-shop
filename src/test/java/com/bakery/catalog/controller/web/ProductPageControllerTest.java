package com.bakery.catalog.controller.web;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.bakery.catalog.dto.ProductDetailResponse;
import com.bakery.catalog.service.ProductQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ProductPageControllerTest {
  private final ProductQueryService products = mock(ProductQueryService.class);

  @Test void legacySearchRouteRedirectsToCanonicalCatalog() throws Exception {
    MockMvcBuilders.standaloneSetup(new ProductPageController(products)).build()
        .perform(get("/products/search").param("keyword", " bánh kem dâu "))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/products?keyword=b%C3%A1nh%20kem%20d%C3%A2u&sort=newest&page=0"));
    verifyNoInteractions(products);
  }

  @Test void legacySearchRoutePreservesSortAndPagination() throws Exception {
    MockMvcBuilders.standaloneSetup(new ProductPageController(products)).build()
        .perform(get("/products/search").param("keyword", "missing").param("sort", "price_asc").param("page", "1"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/products?keyword=missing&sort=price_asc&page=1"));
    verifyNoInteractions(products);
  }

  @Test void detailPassesSelectedProductToTemplate() throws Exception {
    var cake = new ProductDetailResponse();
    cake.setId(8L);
    cake.setName("Bánh kem dâu");
    when(products.getById(8L)).thenReturn(cake);
    MockMvcBuilders.standaloneSetup(new ProductPageController(products)).build()
        .perform(get("/products/8")).andExpect(status().isOk())
        .andExpect(view().name("product/detail")).andExpect(model().attribute("product", cake));
  }
}
