package com.bakery.catalog.controller.web;

import static org.junit.jupiter.api.Assertions.*;

import com.bakery.catalog.dto.CategoryResponse;
import com.bakery.catalog.dto.ProductDetailResponse;
import com.bakery.catalog.dto.ProductImageResponse;
import com.bakery.catalog.entity.ProductStatus;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;

class ProductDetailTemplateTest {
  private ProductDetailResponse product() {
    var product = new ProductDetailResponse();
    product.setId(42L);
    product.setName("Strawberry cake <special>");
    product.setDescription("Fresh strawberry and cream");
    product.setPrice(new BigDecimal("450000"));
    product.setStockQuantity(2);
    product.setPreparationDays(1);
    product.setStatus(ProductStatus.SELLING);
    var category = new CategoryResponse();
    category.setName("Birthday cakes");
    product.setCategory(category);
    return product;
  }

  private String render(ProductDetailResponse product) throws Exception {
    String template = Files.readString(Path.of("src/main/resources/templates/product/detail.html"));
    int start = template.indexOf("<section class=\"max-w-[1280px]");
    int end = template.indexOf("<!-- ACCORDION / TABS DETAILS SECTION");
    var engine = new SpringTemplateEngine();
    engine.setTemplateResolver(new StringTemplateResolver());
    var context = new Context();
    context.setVariable("product", product);
    context.setVariable("_csrf", Map.of("token", "test-token", "headerName", "X-CSRF-TOKEN"));
    return engine.process(template.substring(start, end), context);
  }

  @Test void rendersSelectedProductAndItsImagesInsteadOfFixedCroissant() throws Exception {
    var product = product();
    var image = new ProductImageResponse();
    image.setImagePath("/uploads/strawberry.png");
    product.setImages(List.of(image));
    String html = render(product);
    assertTrue(html.contains("Strawberry cake &lt;special&gt;"));
    assertTrue(html.contains("Fresh strawberry and cream"));
    assertTrue(html.contains("450.000"));
    assertTrue(html.contains("/uploads/strawberry.png"));
    assertTrue(html.contains("data-product-id=\"42\""));
    assertTrue(html.contains("max=\"2\""));
    assertFalse(html.contains("Normandie"));
    assertFalse(html.contains("disabled="));
  }

  @Test void missingImageUsesPlaceholderAndSoldOutDisablesPurchase() throws Exception {
    var product = product();
    product.setStockQuantity(0);
    String html = render(product);
    assertTrue(html.contains("/assets/product-placeholder.svg"));
    assertTrue(html.contains("disabled="));
    assertTrue(html.contains("Sản phẩm tạm hết hàng"));
  }

  @Test void stoppedProductDisablesPurchaseEvenWhenStockRemains() throws Exception {
    var product = product();
    product.setStatus(ProductStatus.STOPPED);
    String html = render(product);
    assertTrue(html.contains("Sản phẩm đã ngừng bán"));
    assertTrue(html.contains("disabled="));
  }
}
