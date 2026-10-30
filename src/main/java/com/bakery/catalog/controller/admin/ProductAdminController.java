package com.bakery.catalog.controller.admin;

import com.bakery.catalog.dto.ProductCreateRequest;
import com.bakery.catalog.dto.ProductDetailResponse;
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
import org.springframework.web.bind.annotation.ModelAttribute;
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
      @RequestParam(required = false) ProductStatus status,
      @RequestParam(defaultValue = "newest") String sort, Model model) {
    model.addAttribute("products", productAdminService.search(keyword, categoryId, status, sort,
        PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))));
    model.addAttribute("categories", categoryAdminService.findAllActive());
    model.addAttribute("keyword", keyword);
    model.addAttribute("categoryId", categoryId);
    model.addAttribute("status", status);
    model.addAttribute("sort", sort);
    return "cms/product/list";
  }

  @GetMapping({"/new", "/add"})
  public String newProduct(Model model) {
    populateProductForm(model, false);
    model.addAttribute("productRequest", new ProductCreateRequest());
    return "cms/product/form";
  }

  @GetMapping("/{id}/edit")
  public String editProduct(@PathVariable Long id, Model model) {
    ProductDetailResponse product = productAdminService.getById(id);
    populateProductForm(model, true);
    model.addAttribute("product", product);
    model.addAttribute("productRequest", toUpdateRequest(product));
    return "cms/product/form";
  }

  @PostMapping(value = "/add", consumes = "multipart/form-data")
  public String createProduct(@ModelAttribute("productRequest") @Valid ProductCreateRequest request,
      BindingResult result, Model model, RedirectAttributes redirect) {
    if (result.hasErrors()) {
      populateProductForm(model, false);
      return "cms/product/form";
    }
    try {
      productAdminService.create(request);
      redirect.addFlashAttribute("successMessage", "Thêm sản phẩm thành công!");
    } catch (RuntimeException exception) {
      redirect.addFlashAttribute("errorMessage", exception.getMessage());
    }
    return "redirect:/cms/products";
  }

  @PostMapping(value = "/update/{id}", consumes = "multipart/form-data")
  public String updateProduct(@PathVariable Long id,
      @ModelAttribute("productRequest") @Valid ProductUpdateRequest request,
      BindingResult result, Model model, RedirectAttributes redirect) {
    if (result.hasErrors()) {
      populateProductForm(model, true);
      model.addAttribute("product", productAdminService.getById(id));
      return "cms/product/form";
    }
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
    return "redirect:/cms/products/" + productId + "/edit";
  }

  private void populateProductForm(Model model, boolean editing) {
    model.addAttribute("categories", categoryAdminService.findAllActive());
    model.addAttribute("statuses", ProductStatus.values());
    model.addAttribute("isEdit", editing);
  }

  private ProductUpdateRequest toUpdateRequest(ProductDetailResponse product) {
    ProductUpdateRequest request = new ProductUpdateRequest();
    request.setName(product.getName());
    request.setDescription(product.getDescription());
    request.setBadge(product.getBadge());
    request.setIngredients(product.getIngredients());
    request.setStorageInstructions(product.getStorageInstructions());
    request.setAllergenInfo(product.getAllergenInfo());
    request.setPrice(product.getPrice());
    request.setStockQuantity(product.getStockQuantity());
    request.setPreparationDays(product.getPreparationDays());
    request.setCategoryId(product.getCategory().getId());
    request.setStatus(product.getStatus());
    return request;
  }

}
