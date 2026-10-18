package com.bakery.catalog.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/cms")
public class AdminCatalogController {

  @GetMapping("/products")
  public String products() {
    return "cms/product/list";
  }

  @GetMapping("/categories")
  public String categories() {
    return "cms/category/list";
  }
}
