package com.bakery.catalog.controller.admin;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.bakery.catalog.service.CategoryAdminService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class CategoryAdminControllerTest {

  @Mock CategoryAdminService categoryAdminService;
  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    mvc = MockMvcBuilders.standaloneSetup(new CategoryAdminController(categoryAdminService)).build();
  }

  @Test
  void rendersCmsCategoryTable() throws Exception {
    when(categoryAdminService.findAll()).thenReturn(List.of());

    mvc.perform(get("/cms/categories"))
        .andExpect(status().isOk())
        .andExpect(view().name("cms/category/list"))
        .andExpect(model().attributeExists("categories", "categoryRequest"));
  }
}
