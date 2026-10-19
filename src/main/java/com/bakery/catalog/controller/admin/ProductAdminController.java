package com.bakery.catalog.controller.admin;

import com.bakery.catalog.dto.CategoryResponse;
import com.bakery.catalog.dto.ProductCreateRequest;
import com.bakery.catalog.dto.ProductUpdateRequest;
import com.bakery.catalog.entity.ProductStatus;
import com.bakery.catalog.repository.CategoryRepository;
import com.bakery.catalog.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class ProductAdminController {
  private final ProductService productService;
  private final CategoryRepository categories;

  @GetMapping
  public String list(@RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "") String keyword,
      @RequestParam(required = false) Long categoryId, Model model) {
    model.addAttribute("products", productService.search(keyword, categoryId, null,
        PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))));
    model.addAttribute("categories", categories.findAll());
    model.addAttribute("keyword", keyword);
    model.addAttribute("categoryId", categoryId);
    model.addAttribute("createRequest", new ProductCreateRequest());
    model.addAttribute("statuses", ProductStatus.values());
    return "cms/product/list";
  }

  @PostMapping(value = "/add", consumes = "multipart/form-data")
  public String add(@Valid ProductCreateRequest request, BindingResult result, RedirectAttributes redirect) {
    if (result.hasErrors()) return errorRedirect(redirect, "Dữ liệu sản phẩm không hợp lệ!");
    try {
      productService.create(request);
      redirect.addFlashAttribute("successMessage", "Thêm sản phẩm thành công!");
    } catch (RuntimeException exception) {
      redirect.addFlashAttribute("errorMessage", exception.getMessage());
    }
    return "redirect:/admin/products";
  }

  @PostMapping(value = "/update/{id}", consumes = "multipart/form-data")
  public String update(@PathVariable Long id, @Valid ProductUpdateRequest request,
      BindingResult result, RedirectAttributes redirect) {
    if (result.hasErrors()) return errorRedirect(redirect, "Dữ liệu sản phẩm không hợp lệ!");
    try {
      productService.update(id, request);
      redirect.addFlashAttribute("successMessage", "Cập nhật sản phẩm thành công!");
    } catch (RuntimeException exception) {
      redirect.addFlashAttribute("errorMessage", exception.getMessage());
    }
    return "redirect:/admin/products";
  }

  @PostMapping("/delete/{id}")
  public String delete(@PathVariable Long id, RedirectAttributes redirect) {
    try {
      productService.deleteOrStopSelling(id);
      redirect.addFlashAttribute("successMessage", "Đã xử lý sản phẩm thành công!");
    } catch (RuntimeException exception) {
      redirect.addFlashAttribute("errorMessage", exception.getMessage());
    }
    return "redirect:/admin/products";
  }

  private String errorRedirect(RedirectAttributes redirect, String message) {
    redirect.addFlashAttribute("errorMessage", message);
    return "redirect:/admin/products";
  }
}