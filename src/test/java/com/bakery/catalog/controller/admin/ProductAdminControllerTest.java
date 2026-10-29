package com.bakery.catalog.controller.admin;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.bakery.catalog.dto.CategoryResponse;
import com.bakery.catalog.dto.ProductDetailResponse;
import com.bakery.catalog.entity.ProductStatus;
import com.bakery.catalog.service.CategoryAdminService;
import com.bakery.catalog.service.ProductAdminService;
import com.bakery.catalog.service.ProductRemovalResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
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
        .andExpect(model().attributeExists("products", "categories"));
  }

  @Test
  void rendersSeparateProductCreatePage() throws Exception {
    when(categoryAdminService.findAllActive()).thenReturn(List.of());

    mvc.perform(get("/cms/products/new"))
        .andExpect(status().isOk())
        .andExpect(view().name("cms/product/form"))
        .andExpect(model().attribute("isEdit", false))
        .andExpect(model().attributeExists("categories", "statuses", "productRequest"));
  }

  @Test
  void rendersProductCreatePageFromAddRoute() throws Exception {
    when(categoryAdminService.findAllActive()).thenReturn(List.of());

    mvc.perform(get("/cms/products/add"))
        .andExpect(status().isOk())
        .andExpect(view().name("cms/product/form"))
        .andExpect(model().attribute("isEdit", false));
  }

  @Test
  void rendersSeparateProductEditPage() throws Exception {
    ProductDetailResponse product = new ProductDetailResponse();
    product.setId(5L);
    product.setName("Bánh kem");
    product.setCategory(new CategoryResponse());
    product.getCategory().setId(2L);
    product.setStatus(ProductStatus.SELLING);
    when(productAdminService.getById(5L)).thenReturn(product);
    when(categoryAdminService.findAllActive()).thenReturn(List.of());

    mvc.perform(get("/cms/products/5/edit"))
        .andExpect(status().isOk())
        .andExpect(view().name("cms/product/form"))
        .andExpect(model().attribute("isEdit", true))
        .andExpect(model().attributeExists("categories", "statuses", "product", "productRequest"));
  }

  @Test
  void keepsCreateFormOpenWhenValidationFails() throws Exception {
    when(categoryAdminService.findAllActive()).thenReturn(List.of());

    mvc.perform(multipart("/cms/products/add")
            .file(new MockMultipartFile("images", "", "application/octet-stream", new byte[0])))
        .andExpect(status().isOk())
        .andExpect(view().name("cms/product/form"))
        .andExpect(model().attribute("isEdit", false))
        .andExpect(model().attributeHasFieldErrors("productRequest", "name", "price", "categoryId"));
  }

  @Test
  void productStatusDeleteAndImageActionsReturnToWorkingPages() throws Exception {
    when(productAdminService.deleteOrStopSelling(5L)).thenReturn(ProductRemovalResult.DELETED);

    mvc.perform(post("/cms/products/toggle-status/5"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/cms/products"));
    mvc.perform(post("/cms/products/delete/5"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/cms/products"));
    mvc.perform(post("/cms/products/update/5/images/11/delete"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/cms/products/5/edit"));

    verify(productAdminService).toggleStatus(5L);
    verify(productAdminService).deleteOrStopSelling(5L);
    verify(productAdminService).deleteImage(5L, 11L);
  }
}
