package com.bakery.catalog.controller.admin;

import com.bakery.catalog.dto.ProductCreateRequest;
import com.bakery.catalog.dto.ProductUpdateRequest;
import com.bakery.catalog.entity.ProductStatus;
import com.bakery.catalog.service.CategoryAdminService;
import com.bakery.catalog.service.ProductAdminService;
import com.bakery.catalog.service.ProductRemovalResult;
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
@RequestMapping("/cms/products")
@RequiredArgsConstructor
public class ProductAdminController {
  private final ProductAdminService productAdminService;
  private final CategoryAdminService categoryAdminService;

  @GetMapping
  public String getAllProducts(@RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "") String keyword,
      @RequestParam(required = false) Long categoryId,
      @RequestParam(required = false) ProductStatus status, Model model) {
    model.addAttribute("products", productAdminService.search(keyword, categoryId, status,
        PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))));
    model.addAttribute("categories", categoryAdminService.findAllActive());
    model.addAttribute("keyword", keyword);
    model.addAttribute("categoryId", categoryId);
    model.addAttribute("status", status);
    model.addAttribute("createRequest", new ProductCreateRequest());
    model.addAttribute("statuses", ProductStatus.values());
    return "cms/product/list";
  }

  @PostMapping(value = "/add", consumes = "multipart/form-data")
  public String createProduct(@Valid ProductCreateRequest request, BindingResult result, RedirectAttributes redirect) {
    if (result.hasErrors()) return errorRedirect(redirect, "Dữ liệu sản phẩm không hợp lệ!");
    try {
      productAdminService.create(request);
      redirect.addFlashAttribute("successMessage", "Thêm sản phẩm thành công!");
    } catch (RuntimeException exception) {
      redirect.addFlashAttribute("errorMessage", exception.getMessage());
    }
    return "redirect:/cms/products";
  }

  @PostMapping(value = "/update/{id}", consumes = "multipart/form-data")
  public String updateProduct(@PathVariable Long id, @Valid ProductUpdateRequest request,
      BindingResult result, RedirectAttributes redirect) {
    if (result.hasErrors()) return errorRedirect(redirect, "Dữ liệu sản phẩm không hợp lệ!");
    try {
      productAdminService.update(id, request);
      redirect.addFlashAttribute("successMessage", "Cập nhật sản phẩm thành công!");
    } catch (RuntimeException exception) {
      redirect.addFlashAttribute("errorMessage", exception.getMessage());
    }
    return "redirect:/cms/products";
  }

  @PostMapping("/delete/{id}")
  public String deleteProduct(@PathVariable Long id, RedirectAttributes redirect) {
    try {
      ProductRemovalResult result = productAdminService.deleteOrStopSelling(id);
      redirect.addFlashAttribute("successMessage", result == ProductRemovalResult.DELETED
          ? "Xóa sản phẩm thành công."
          : "Sản phẩm đã phát sinh đơn hàng nên được chuyển sang ngừng bán.");
    } catch (RuntimeException exception) {
      redirect.addFlashAttribute("errorMessage", exception.getMessage());
    }
    return "redirect:/cms/products";
  }

  @PostMapping("/toggle-status/{id}")
  public String toggleProductStatus(@PathVariable Long id, RedirectAttributes redirect) {
    try {
      productAdminService.toggleStatus(id);
      redirect.addFlashAttribute("successMessage", "Đã cập nhật trạng thái sản phẩm.");
    } catch (RuntimeException exception) {
      redirect.addFlashAttribute("errorMessage", exception.getMessage());
    }
    return "redirect:/cms/products";
  }

  @PostMapping("/update/{productId}/images/{imageId}/delete")
  public String deleteProductImage(@PathVariable Long productId, @PathVariable Long imageId,
      RedirectAttributes redirect) {
    try {
      productAdminService.deleteImage(productId, imageId);
      redirect.addFlashAttribute("successMessage", "Đã xóa ảnh sản phẩm.");
    } catch (RuntimeException exception) {
      redirect.addFlashAttribute("errorMessage", exception.getMessage());
    }
    return "redirect:/cms/products";
  }

  private String errorRedirect(RedirectAttributes redirect, String message) {
    redirect.addFlashAttribute("errorMessage", message);
    return "redirect:/cms/products";
  }

}
