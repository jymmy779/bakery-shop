package com.bakery.catalog.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/products")
public class ProductController {

  @GetMapping
  public String list() {
    return "product/list";
  }

  @GetMapping("/{productId}")
  public String detail(@PathVariable Long productId, Model model) {
    model.addAttribute("productId", productId);
    return "product/detail";
  }
}
