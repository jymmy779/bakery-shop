package com.bakery.catalog.controller.web;

import com.bakery.catalog.service.ProductQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequiredArgsConstructor
public class ProductPageController {

  private final ProductQueryService productQueryService;

  @GetMapping("/products")
  public String list() {
    return "product/list";
  }

  @GetMapping("/products/search")
  public String search(@RequestParam(defaultValue = "") String keyword,
      @RequestParam(defaultValue = "newest") String sort,
      @RequestParam(defaultValue = "0") int page) {
    keyword = keyword.strip();
    return "redirect:" + UriComponentsBuilder.fromPath("/products")
        .queryParam("keyword", keyword)
        .queryParam("sort", sort)
        .queryParam("page", Math.max(0, page))
        .build().encode().toUriString();
  }

  @GetMapping("/products/{productId}")
  public String detail(@PathVariable Long productId, Model model) {
    model.addAttribute("product", productQueryService.getById(productId));
    return "product/detail";
  }
}
