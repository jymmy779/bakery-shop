package com.bakery.catalog.controller.web;

import com.bakery.catalog.dto.ProductSummaryResponse;
import com.bakery.catalog.service.CatalogQueryService;
import java.util.List;
import java.math.BigDecimal;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
    when(catalog.filter("missing", null, null, null, "name", 0, 12))
        .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 12), 0));
    MockMvcBuilders.standaloneSetup(new ProductController(catalog)).build()
        .perform(get("/api/products").param("keyword", "missing"))
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
    when(catalog.filter("dau", null, null, null, "name", 0, 12))
        .thenReturn(new PageImpl<>(List.of(product), PageRequest.of(0, 12), 1));
    MockMvcBuilders.standaloneSetup(new ProductController(catalog)).build()
        .perform(get("/api/products").param("keyword", "dau"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Tìm thấy 1 sản phẩm"))
        .andExpect(jsonPath("$.products[0].id").value(1))
        .andExpect(jsonPath("$.products[0].name").value("Bánh dâu"));
  }

  @Test
  void nextAndPreviousLinksKeepAllFilters() throws Exception {
    var product = new ProductSummaryResponse();
    var min = new BigDecimal("25000");
    var max = new BigDecimal("300000");
    when(catalog.filter("dâu", 1L, min, max, "price_desc", 1, 1))
        .thenReturn(new PageImpl<>(List.of(product), PageRequest.of(1, 1), 3));
    MockMvcBuilders.standaloneSetup(new ProductController(catalog)).build()
        .perform(get("/api/products").param("keyword", "dâu").param("categoryId", "1")
            .param("minPrice", "25000").param("maxPrice", "300000")
            .param("sort", "price_desc").param("page", "1").param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(3))
        .andExpect(jsonPath("$.filters.categoryId").value(1))
        .andExpect(jsonPath("$.next").value("/products?keyword=d%C3%A2u&sort=price_desc&page=2&size=1&categoryId=1&minPrice=25000&maxPrice=300000"))
        .andExpect(jsonPath("$.previous").value("/products?keyword=d%C3%A2u&sort=price_desc&page=0&size=1&categoryId=1&minPrice=25000&maxPrice=300000"));
  }

  @Test
  void malformedFilterReturnsJson400() throws Exception {
    MockMvcBuilders.standaloneSetup(new ProductController(catalog)).build()
        .perform(get("/api/products").param("minPrice", "abc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }
}
