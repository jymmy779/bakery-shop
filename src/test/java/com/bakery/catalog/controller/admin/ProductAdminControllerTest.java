package com.bakery.catalog.controller.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.bakery.catalog.entity.ProductStatus;
import com.bakery.catalog.service.CategoryAdminService;
import com.bakery.catalog.service.ProductAdminService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ProductAdminControllerTest {

  @Mock ProductAdminService productAdminService;
  @Mock CategoryAdminService categoryAdminService;
  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    mvc = MockMvcBuilders.standaloneSetup(
        new ProductAdminController(productAdminService, categoryAdminService)).build();
  }

  @Test
  void rendersCmsProductTableWithFilters() throws Exception {
    when(productAdminService.search("cake", 2L, ProductStatus.SELLING,
        org.springframework.data.domain.PageRequest.of(0, 10)))
        .thenReturn(new PageImpl<>(List.of()));
    when(categoryAdminService.findAllActive()).thenReturn(List.of());

    mvc.perform(get("/cms/products").param("keyword", "cake")
            .param("categoryId", "2").param("status", "SELLING"))
        .andExpect(status().isOk())
        .andExpect(view().name("cms/product/list"))
        .andExpect(model().attributeExists("products", "categories", "statuses"));
  }
}
