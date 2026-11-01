package com.bakery.catalog.controller.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bakery.catalog.dto.CategoryResponse;
import com.bakery.catalog.service.CategoryAdminService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CategoryControllerTest {

  private final CategoryAdminService categoryService = mock(CategoryAdminService.class);

  @Test
  void returnsActiveCategories() throws Exception {
    CategoryResponse category = new CategoryResponse();
    category.setId(1L);
    category.setName("Bánh Kem & Bánh Tiệc");
    category.setActive(true);
    when(categoryService.findAllActive()).thenReturn(List.of(category));

    MockMvcBuilders.standaloneSetup(new CategoryController(categoryService)).build()
        .perform(get("/api/categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[0].name").value("Bánh Kem & Bánh Tiệc"))
        .andExpect(jsonPath("$[0].active").value(true));
  }
}
