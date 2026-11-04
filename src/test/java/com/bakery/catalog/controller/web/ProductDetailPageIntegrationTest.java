package com.bakery.catalog.controller.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

import com.bakery.catalog.dto.CategoryResponse;
import com.bakery.catalog.dto.ProductDetailResponse;
import com.bakery.catalog.entity.ProductStatus;
import com.bakery.catalog.service.ProductQueryService;
import com.bakery.config.SecurityConfig;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductPageController.class)
@Import(SecurityConfig.class)
class ProductDetailPageIntegrationTest {
  @Autowired MockMvc mvc;
  @MockitoBean ProductQueryService products;
  @MockitoBean com.bakery.account.repository.AccountRepository accounts;

  @Test void guestCanRenderEntireDetailPageWithCsrfToken() throws Exception {
    var product = new ProductDetailResponse();
    product.setId(1L);
    product.setName("Bánh kem dâu");
    product.setDescription("Bánh kem dâu từ database");
    product.setPrice(new BigDecimal("450000"));
    product.setStatus(ProductStatus.SELLING);
    product.setStockQuantity(3);
    var category = new CategoryResponse();
    category.setName("Bánh kem");
    product.setCategory(category);
    when(products.getById(1L)).thenReturn(product);
    var result = mvc.perform(get("/products/1"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Bánh kem dâu từ database")))
        .andExpect(content().string(containsString("cart-csrf-token")))
        .andExpect(content().string(containsString("/js/product-detail.js")))
        .andReturn();
    String html = result.getResponse().getContentAsString();
    org.junit.jupiter.api.Assertions.assertTrue(html.indexOf("name=\"_csrf\"") < 1024);
    org.junit.jupiter.api.Assertions.assertTrue(html.contains("</html>"));
  }
}
