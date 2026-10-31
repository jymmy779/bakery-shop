package com.bakery.catalog.controller.admin;

import com.bakery.catalog.dto.CategoryRequest;
import com.bakery.catalog.dto.CategoryResponse;
import com.bakery.catalog.service.CategoryAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cms/categories")
@RequiredArgsConstructor
public class CategoryAdminController {
  private final CategoryAdminService categoryAdminService;

  @GetMapping
  public String getAllCategories(Model model) {
    model.addAttribute("categories", categoryAdminService.findAll());
    return "cms/category/list";
  }

  @GetMapping({"/add", "/new"})
  public String newCategory(Model model) {
    populateCategoryForm(model, false);
    model.addAttribute("categoryRequest", new CategoryRequest());
    return "cms/category/form";
  }

  @GetMapping("/{id}/edit")
  public String editCategory(@PathVariable Long id, Model model) {
    CategoryResponse category = categoryAdminService.getById(id);
    model.addAttribute("category", category);
    model.addAttribute("categoryRequest", toCategoryRequest(category));
    populateCategoryForm(model, true);
    return "cms/category/form";
  }

  @PostMapping("/toggle-status/{id}")
  public String toggleCategoryStatus(@PathVariable Long id, RedirectAttributes redirect) {
    try {
      categoryAdminService.toggleStatus(id);
      redirect.addFlashAttribute("successMessage", "Đã cập nhật trạng thái danh mục.");
    } catch (RuntimeException exception) {
      redirect.addFlashAttribute("errorMessage", exception.getMessage());
    }
    return "redirect:/cms/categories";
  }

  @PostMapping("/add")
  public String createCategory(@ModelAttribute("categoryRequest") @Valid CategoryRequest request,
      BindingResult result, Model model, RedirectAttributes redirect) {
    if (result.hasErrors()) {
      populateCategoryForm(model, false);
      return "cms/category/form";
    }
    try {
      categoryAdminService.create(request);
      redirect.addFlashAttribute("successMessage", "Thêm danh mục thành công!");
    } catch (RuntimeException exception) {
      populateCategoryForm(model, false);
      model.addAttribute("errorMessage", exception.getMessage());
      return "cms/category/form";
    }
    return "redirect:/cms/categories";
  }

  @PostMapping("/update/{id}")
  public String updateCategory(@PathVariable Long id,
      @ModelAttribute("categoryRequest") @Valid CategoryRequest request, BindingResult result,
      Model model, RedirectAttributes redirect) {
    if (result.hasErrors()) {
      model.addAttribute("category", categoryAdminService.getById(id));
      populateCategoryForm(model, true);
      return "cms/category/form";
    }
    try {
      categoryAdminService.update(id, request);
      redirect.addFlashAttribute("successMessage", "Cập nhật danh mục thành công!");
    } catch (RuntimeException exception) {
      model.addAttribute("category", categoryAdminService.getById(id));
      populateCategoryForm(model, true);
      model.addAttribute("errorMessage", exception.getMessage());
      return "cms/category/form";
    }
    return "redirect:/cms/categories";
  }

  @PostMapping("/delete/{id}")
  public String deleteCategory(@PathVariable Long id, RedirectAttributes redirect) {
    try {
      categoryAdminService.delete(id);
      redirect.addFlashAttribute("successMessage", "Xóa danh mục thành công!");
    } catch (RuntimeException exception) {
      redirect.addFlashAttribute("errorMessage", exception.getMessage());
    }
    return "redirect:/cms/categories";
  }

  private void populateCategoryForm(Model model, boolean editing) {
    model.addAttribute("isEdit", editing);
  }

  private CategoryRequest toCategoryRequest(CategoryResponse category) {
    CategoryRequest request = new CategoryRequest();
    request.setName(category.getName());
    request.setDescription(category.getDescription());
    return request;
  }
}
