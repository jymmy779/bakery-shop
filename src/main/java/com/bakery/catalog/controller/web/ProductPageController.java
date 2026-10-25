package com.bakery.catalog.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ProductPageController {

  @GetMapping("/products")
  public String list() {
    return "product/list";
  }

  @GetMapping("/products/{productId}")
  public String detail(@PathVariable Long productId) {
    return "product/detail";
  }
}
