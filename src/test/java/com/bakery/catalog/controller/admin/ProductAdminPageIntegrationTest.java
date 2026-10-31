package com.bakery.catalog.controller.admin;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bakery.account.entity.Account;
import com.bakery.account.entity.AccountStatus;
import com.bakery.account.entity.Role;
import com.bakery.account.repository.AccountRepository;
import com.bakery.catalog.dto.CategoryResponse;
import com.bakery.catalog.dto.ProductDetailResponse;
import com.bakery.catalog.dto.ProductImageResponse;
import com.bakery.catalog.dto.ProductSummaryResponse;
import com.bakery.catalog.entity.ProductStatus;
import com.bakery.catalog.service.CategoryAdminService;
import com.bakery.catalog.service.ProductAdminService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
    "app.bootstrap-admin.enabled=false",
    "spring.jpa.show-sql=false",
    "logging.level.root=WARN",
    "logging.level.com.bakery=WARN"
})
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "cms-admin@example.test", roles = "ADMIN")
class ProductAdminPageIntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired AccountRepository accountRepository;
  @MockitoBean ProductAdminService productAdminService;
  @MockitoBean CategoryAdminService categoryAdminService;

  @BeforeEach
  void setUp() {
    Account account = accountRepository.findByEmailIgnoreCase("cms-admin@example.test")
        .orElseGet(Account::new);
    account.setEmail("cms-admin@example.test");
    account.setPasswordHash("test-password-hash");
    account.setFullName("CMS Test Admin");
    account.setRole(Role.ROLE_ADMIN);
    account.setStatus(AccountStatus.ACTIVE);
    accountRepository.save(account);
    when(categoryAdminService.findAllActive()).thenReturn(List.of(category()));
    when(productAdminService.getById(anyLong())).thenReturn(product());
  }

  @Test
  void rendersProductCreatePage() throws Exception {
    mvc.perform(get("/cms/products/add"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Thêm sản phẩm")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/cms/products/add\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"product-saving\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"product-upload-progress\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Đang tải ảnh lên...")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"price-suggestions\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("[100, 1000, 10000]")));
  }

  @Test
  void rendersProductListWithWorkingActionDestinations() throws Exception {
    ProductSummaryResponse summary = new ProductSummaryResponse();
    summary.setId(42L);
    summary.setName("Bánh kem thử nghiệm");
    summary.setDescription("Mô tả sản phẩm");
    summary.setPrice(new BigDecimal("125000"));
    summary.setStockQuantity(5);
    summary.setStatus(ProductStatus.SELLING);
    summary.setCategoryName("Bánh kem");
    when(productAdminService.search("", null, null, null, "newest", PageRequest.of(0, 10)))
        .thenReturn(new PageImpl<>(List.of(summary)));

    mvc.perform(get("/cms/products"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/cms/products/add\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("CMS Test Admin")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("placeholder=\"Tra cứu tên sản phẩm...\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("aria-label=\"Sắp xếp sản phẩm\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"newest\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Mới nhất")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("aria-label=\"Tra cứu theo tên sản phẩm\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("viewBox=\"0 0 24 24\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/cms/products/42/edit\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/cms/products/toggle-status/42\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/cms/products/delete/42\"")))
        .andExpect(content().string(org.hamcrest.Matchers.not(
            org.hamcrest.Matchers.containsString("<footer"))))
        .andExpect(content().string(org.hamcrest.Matchers.not(
            org.hamcrest.Matchers.containsString("<header class=\"cms-header\">"))));
  }

  @Test
  void rendersCategoryActionsWithTheirControllerRoutes() throws Exception {
    CategoryResponse category = new CategoryResponse();
    category.setId(3L);
    category.setName("Bánh kem");
    category.setDescription("Danh mục bánh kem");
    category.setActive(true);
    when(categoryAdminService.findAll()).thenReturn(List.of(category));

    mvc.perform(get("/cms/categories"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("CMS Test Admin")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("placeholder=\"Tra cứu tên danh mục...\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("data-category-row")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("data-category-no-results")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("viewBox=\"0 0 24 24\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/cms/categories/add\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/cms/categories/3/edit\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/cms/categories/toggle-status/3\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/cms/categories/delete/3\"")))
        .andExpect(content().string(org.hamcrest.Matchers.not(
            org.hamcrest.Matchers.containsString("category-sidebar"))))
        .andExpect(content().string(org.hamcrest.Matchers.not(
            org.hamcrest.Matchers.containsString("<footer"))))
        .andExpect(content().string(org.hamcrest.Matchers.not(
            org.hamcrest.Matchers.containsString("<header class=\"cms-header\">"))));
  }

  @Test
  void rendersCategoryCreateFormOnSeparatePage() throws Exception {
    mvc.perform(get("/cms/categories/add"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Thêm danh mục")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/cms/categories/add\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"name\"")));
  }

  @Test
  void rendersCategoryUpdateFormWithExistingValuesOnSeparatePage() throws Exception {
    CategoryResponse category = category();
    category.setName("Category Name");
    category.setDescription("Special description");
    when(categoryAdminService.getById(3L)).thenReturn(category);

    mvc.perform(get("/cms/categories/3/edit"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Cập nhật danh mục")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/cms/categories/update/3\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"Category Name\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Special description")));
  }

  @Test
  void rendersProductEditPageWithExistingImages() throws Exception {
    ProductDetailResponse product = product();
    ProductImageResponse firstImage = new ProductImageResponse();
    firstImage.setId(11L);
    firstImage.setImagePath("/uploads/cake-front.jpg");
    firstImage.setDisplayOrder(1);
    ProductImageResponse secondImage = new ProductImageResponse();
    secondImage.setId(12L);
    secondImage.setImagePath("/uploads/cake-side.jpg");
    secondImage.setDisplayOrder(2);
    product.setImages(List.of(firstImage, secondImage));
    when(productAdminService.getById(42L)).thenReturn(product);

    mvc.perform(get("/cms/products/42/edit"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Cập nhật sản phẩm")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("/uploads/cake-front.jpg")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/cms/products/update/42\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("data-is-edit=\"true\"")));
  }

  private ProductDetailResponse product() {
    ProductDetailResponse product = new ProductDetailResponse();
    product.setId(42L);
    product.setName("Bánh kem thử nghiệm");
    product.setDescription("Mô tả sản phẩm");
    product.setPrice(new BigDecimal("125000"));
    product.setStockQuantity(5);
    product.setPreparationDays(1);
    product.setStatus(ProductStatus.SELLING);
    product.setCategory(category());
    return product;
  }

  private CategoryResponse category() {
    CategoryResponse category = new CategoryResponse();
    category.setId(3L);
    category.setName("Bánh kem");
    category.setActive(true);
    return category;
  }
}
