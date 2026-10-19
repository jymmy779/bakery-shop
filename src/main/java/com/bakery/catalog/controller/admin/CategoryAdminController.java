package com.bakery.catalog.controller.admin;

import com.bakery.catalog.dto.CategoryRequest;
import com.bakery.catalog.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class CategoryAdminController {
  private final CategoryService categoryService;

  @GetMapping
  public String getAllCategories(Model model) {
    model.addAttribute("categories", categoryService.getAllCategories());
    model.addAttribute("categoryRequest", new CategoryRequest());
    return "cms/category/list";
  }

  @PostMapping("/add")
  public String createCategory(@Valid CategoryRequest request, BindingResult result, RedirectAttributes redirect) {
    if (result.hasErrors()) return errorRedirect(redirect, "Dữ liệu danh mục không hợp lệ!");
    try {
      categoryService.createCategory(request);
      redirect.addFlashAttribute("successMessage", "Thêm danh mục thành công!");
    } catch (RuntimeException exception) {
      redirect.addFlashAttribute("errorMessage", exception.getMessage());
    }
    return "redirect:/admin/categories";
  }

  @PostMapping("/update/{id}")
  public String updateCategory(@PathVariable Long id, @Valid CategoryRequest request, BindingResult result,
      RedirectAttributes redirect) {
    if (result.hasErrors()) return errorRedirect(redirect, "Dữ liệu danh mục không hợp lệ!");
    try {
      categoryService.updateCategory(id, request);
      redirect.addFlashAttribute("successMessage", "Cập nhật danh mục thành công!");
    } catch (RuntimeException exception) {
      redirect.addFlashAttribute("errorMessage", exception.getMessage());
    }
    return "redirect:/admin/categories";
  }

  @PostMapping("/delete/{id}")
  public String deleteCategory(@PathVariable Long id, RedirectAttributes redirect) {
    try {
      categoryService.deleteCategory(id);
      redirect.addFlashAttribute("successMessage", "Xóa danh mục thành công!");
    } catch (RuntimeException exception) {
      redirect.addFlashAttribute("errorMessage", exception.getMessage());
    }
    return "redirect:/admin/categories";
  }

  private String errorRedirect(RedirectAttributes redirect, String message) {
    redirect.addFlashAttribute("errorMessage", message);
    return "redirect:/admin/categories";
  }
}