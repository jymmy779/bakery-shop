package com.bakery.catalog.controller.web;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.bakery.catalog.dto.ProductDetailResponse;
import com.bakery.catalog.dto.ProductSummaryResponse;
import com.bakery.catalog.service.ProductQueryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ProductPageControllerTest {
  private final ProductQueryService products = mock(ProductQueryService.class);

  @Test void searchPassesKeywordToDatabaseServiceAndDisplaysItsResults() throws Exception {
    var cake = new ProductSummaryResponse();
    cake.setId(1L);
    cake.setName("Bánh kem dâu");
    var results = new PageImpl<>(List.of(cake), PageRequest.of(0, 12), 1);
    when(products.filter("bánh kem dâu", null, null, null, "newest", 0, 12)).thenReturn(results);
    MockMvcBuilders.standaloneSetup(new ProductPageController(products)).build()
        .perform(get("/products/search").param("keyword", " bánh kem dâu "))
        .andExpect(status().isOk()).andExpect(view().name("product/search"))
        .andExpect(model().attribute("keyword", "bánh kem dâu"))
        .andExpect(model().attribute("results", results));
    verify(products).filter("bánh kem dâu", null, null, null, "newest", 0, 12);
  }

  @Test void searchPreservesSortAndPaginationWithNoMatches() throws Exception {
    var results = new PageImpl<ProductSummaryResponse>(List.of(), PageRequest.of(1, 12), 0);
    when(products.filter("missing", null, null, null, "price_asc", 1, 12)).thenReturn(results);
    MockMvcBuilders.standaloneSetup(new ProductPageController(products)).build()
        .perform(get("/products/search").param("keyword", "missing").param("sort", "price_asc").param("page", "1"))
        .andExpect(status().isOk()).andExpect(model().attribute("results", results))
        .andExpect(model().attribute("sort", "price_asc"));
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
