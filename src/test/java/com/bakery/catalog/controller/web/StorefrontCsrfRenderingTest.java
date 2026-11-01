package com.bakery.catalog.controller.web;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bakery.cart.controller.web.CartController;
import com.bakery.catalog.dto.CategoryResponse;
import com.bakery.catalog.dto.ProductDetailResponse;
import com.bakery.catalog.service.ProductQueryService;
import com.bakery.config.SecurityConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import jakarta.servlet.http.HttpSession;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@SpringJUnitWebConfig(StorefrontCsrfRenderingTest.Config.class)
@org.springframework.security.test.context.support.WithMockUser(roles = "CUSTOMER")
class StorefrontCsrfRenderingTest {
  @Configuration
  @EnableWebMvc
  @Import({SecurityConfig.class, ProductPageController.class, CartController.class})
  static class Config {
    @Bean ProductQueryService products() { return mock(ProductQueryService.class); }
    @Bean UserDetailsService users() { return mock(UserDetailsService.class); }
    @Bean ThymeleafViewResolver views() {
      var templates = new ClassLoaderTemplateResolver();
      templates.setPrefix("templates/");
      templates.setSuffix(".html");
      templates.setTemplateMode("HTML");
      templates.setCharacterEncoding("UTF-8");
      var engine = new SpringTemplateEngine();
      engine.setTemplateResolver(templates);
      engine.addDialect(new SpringSecurityDialect());
      var views = new ThymeleafViewResolver();
      views.setTemplateEngine(engine);
      views.setCharacterEncoding("UTF-8");
      return views;
    }
  }

  @Autowired WebApplicationContext context;
  @Autowired ProductQueryService products;
  MockMvc mvc;

  @BeforeEach
  void setup() {
    var product = new ProductDetailResponse();
    product.setId(1L);
    product.setName("Bánh thử nghiệm");
    product.setPrice(new BigDecimal("120000"));
    product.setStockQuantity(5);
    var category = new CategoryResponse();
    category.setName("Bánh kem");
    product.setCategory(category);
    when(products.getById(1L)).thenReturn(product);
    mvc = MockMvcBuilders.webAppContextSetup(context)
        // Model a servlet response that starts streaming before a large template finishes.
        .addFilter((request, response, chain) -> {
          var httpResponse = (HttpServletResponse) response;
          var guardedRequest = new HttpServletRequestWrapper((HttpServletRequest) request) {
            @Override public HttpSession getSession(boolean create) {
              if (create && super.getSession(false) == null && httpResponse.isCommitted()) {
                throw new IllegalStateException("Cannot create a session after response commit");
              }
              return super.getSession(create);
            }
            @Override public HttpSession getSession() { return getSession(true); }
          };
          var streamingResponse = new HttpServletResponseWrapper(httpResponse) {
            @Override public PrintWriter getWriter() throws IOException {
              return new PrintWriter(new FilterWriter(super.getWriter()) {
                private int written;
                @Override public void write(char[] chars, int offset, int length) throws IOException {
                  super.write(chars, offset, length);
                  written += length;
                  if (written > 2048) httpResponse.flushBuffer();
                }
              });
            }
          };
          chain.doFilter(guardedRequest, streamingResponse);
        })
        .apply(springSecurity())
        .build();
  }

  @ParameterizedTest
  @ValueSource(strings = {"/products", "/products/search", "/products/1", "/cart"})
  void freshSessionCanRenderPageAndCreateCsrfSessionBeforeStreaming(String path) throws Exception {
    var result = mvc.perform(get(path)).andExpect(status().isOk()).andReturn();
    assertNotNull(result.getRequest().getSession(false));
    org.junit.jupiter.api.Assertions.assertTrue(
        result.getResponse().getContentAsString().contains("name=\"_csrf\" content=\""));
  }
}
