package com.bakery.catalog.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/cms")
public class AdminCatalogController {

  //Lấy thẻ từ products
  @GetMapping("/products")
  public String products() {
    return "redirect:/admin/products";
  }

  @GetMapping("/categories")
  public String categories() {
    return "redirect:/admin/categories";
  }
}
