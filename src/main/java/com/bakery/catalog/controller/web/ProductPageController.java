package com.bakery.catalog.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ProductPageController {

  @GetMapping("/products")
  public String list() {
    return "product/list";
  }

  @GetMapping("/products/search")
  public String search(@RequestParam(defaultValue = "") String keyword, Model model) {
    model.addAttribute("keyword", keyword.strip());
    return "product/search";
  }

  @GetMapping("/products/{productId}")
  public String detail(@PathVariable Long productId) {
    return "product/detail";
  }
}
