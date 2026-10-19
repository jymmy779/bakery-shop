package com.bakery.catalog.controller.web;

import com.bakery.catalog.dto.ProductSummaryResponse;
import com.bakery.catalog.service.CatalogQueryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProductControllerTest {
  private final CatalogQueryService catalog = mock(CatalogQueryService.class);

  @Test
  void noMatchesReturnsRequiredMessageAndEmptyProducts() throws Exception {
    when(catalog.list("missing")).thenReturn(List.of());
    MockMvcBuilders.standaloneSetup(new ProductController(catalog)).build()
        .perform(get("/products").param("keyword", "missing"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").value("Không tìm thấy sản phẩm phù hợp"))
        .andExpect(jsonPath("$.products").isEmpty());
  }

  @Test
  void matchesKeepTheSameResponseStructure() throws Exception {
    var product = new ProductSummaryResponse();
    product.setId(1L);
    product.setName("Bánh dâu");
    when(catalog.list("dau")).thenReturn(List.of(product));
    MockMvcBuilders.standaloneSetup(new ProductController(catalog)).build()
        .perform(get("/products").param("keyword", "dau"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Tìm thấy 1 sản phẩm"))
        .andExpect(jsonPath("$.products[0].id").value(1))
        .andExpect(jsonPath("$.products[0].name").value("Bánh dâu"));
  }
}
