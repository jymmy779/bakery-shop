package com.bakery.catalog.service.impl;

import com.bakery.catalog.dto.ProductCreateRequest;
import com.bakery.catalog.dto.ProductDetailResponse;
import com.bakery.catalog.dto.ProductSummaryResponse;
import com.bakery.catalog.dto.ProductUpdateRequest;
import com.bakery.catalog.entity.Product;
import com.bakery.catalog.entity.ProductImage;
import com.bakery.catalog.entity.ProductStatus;
import com.bakery.catalog.repository.CategoryRepository;
import com.bakery.catalog.repository.ProductImageRepository;
import com.bakery.catalog.repository.ProductRepository;
import com.bakery.common.exception.ResourceNotFoundException;
import com.bakery.catalog.service.ProductQueryService;
import com.bakery.catalog.service.ProductAdminService;
import com.bakery.catalog.service.ProductRemovalResult;
import com.bakery.order.service.ProductOrderReferenceService;
import com.bakery.storage.FileStorageService;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProductAdminServiceImpl implements ProductAdminService {
  private static final int MAX_IMAGE_COUNT = 4;
  private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;
  private final ProductRepository products;
  private final ProductImageRepository images;
  private final CategoryRepository categories;
  private final ProductOrderReferenceService orderReferences;
  private final ProductQueryService productQueryService;
  private final FileStorageService fileStorage;

  @Override
  @Transactional
  public ProductDetailResponse create(ProductCreateRequest request) {
    List<MultipartFile> validImages = validateImages(request.getImages(), 0, true);
    Product product = new Product();
    copy(product, request.getName(), request.getDescription(), request.getPrice(),
        request.getStockQuantity(), request.getPreparationDays(), ProductStatus.SELLING,
        request.getCategoryId());
    List<String> storedPaths = new ArrayList<>();
    try {
      product = products.save(product);
      saveImages(product, validImages, storedPaths);
      return productQueryService.getById(product.getId());
    } catch (RuntimeException exception) {
      deleteStoredFiles(storedPaths, exception);
      throw exception;
    }
  }

  @Override
  @Transactional
  public ProductDetailResponse update(Long productId, ProductUpdateRequest request) {
    Product product = find(productId);
    int existingImageCount = images.findByProductIdInOrderByDisplayOrderAscIdAsc(List.of(productId)).size();
    List<MultipartFile> validImages = validateImages(request.getNewImages(), existingImageCount, false);
    copy(product, request.getName(), request.getDescription(), request.getPrice(),
        request.getStockQuantity(), request.getPreparationDays(), request.getStatus(),
        request.getCategoryId());
    List<String> storedPaths = new ArrayList<>();
    try {
      products.save(product);
      saveImages(product, validImages, storedPaths);
      return productQueryService.getById(productId);
    } catch (RuntimeException exception) {
      deleteStoredFiles(storedPaths, exception);
      throw exception;
    }
  }

  @Override
  @Transactional(readOnly = true)
  public ProductDetailResponse getById(Long productId) {
    return productQueryService.getById(productId);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ProductSummaryResponse> search(String keyword, Long categoryId,
      ProductStatus status, Pageable pageable) {
    String normalized = keyword == null || keyword.isBlank() ? null : keyword.trim();
    Page<Product> page = products.searchAdmin(normalized, categoryId, status, pageable);
    List<Product> content = page.getContent();
    var productImages = new HashMap<Long, List<String>>();
    var productImageIds = new HashMap<Long, List<Long>>();
    if (!content.isEmpty()) {
      images.findByProductIdInOrderByDisplayOrderAscIdAsc(
          content.stream().map(Product::getId).toList())
          .forEach(image -> {
            productImages.computeIfAbsent(image.getProduct().getId(),
                ignored -> new ArrayList<>()).add(image.getImagePath());
            productImageIds.computeIfAbsent(image.getProduct().getId(),
                ignored -> new ArrayList<>()).add(image.getId());
          });
    }
    List<ProductSummaryResponse> summaries = content.stream()
        .map(product -> summary(product,
            productImages.getOrDefault(product.getId(), List.of()),
            productImageIds.getOrDefault(product.getId(), List.of()))).toList();
    return new PageImpl<>(summaries, page.getPageable(), page.getTotalElements());
  }

  @Override
  @Transactional
  public void toggleStatus(Long productId) {
    Product product = find(productId);
    product.setStatus(product.getStatus() == ProductStatus.SELLING
        ? ProductStatus.STOPPED : ProductStatus.SELLING);
  }

  @Override
  @Transactional
  public void deleteImage(Long productId, Long imageId) {
    Product product = find(productId);
    ProductImage image = images.findById(imageId)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hình ảnh"));
    if (!image.getProduct().getId().equals(product.getId())) {
      throw new IllegalArgumentException("Hình ảnh không thuộc sản phẩm này");
    }
    List<ProductImage> productImages =
        images.findByProductIdInOrderByDisplayOrderAscIdAsc(List.of(productId));
    if (productImages.size() <= 1) {
      throw new IllegalStateException("Sản phẩm phải có tối thiểu một ảnh.");
    }
    fileStorage.delete(image.getImagePath());
    images.delete(image);
  }

  @Transactional
  private void deleteProduct(Long productId) {
    Product product = find(productId);
    if (orderReferences.hasOrders(productId)) {
      throw new IllegalStateException(
          "Sản phẩm đã từng có trong đơn hàng, hãy dùng chức năng Ngừng bán để bảo toàn lịch sử mua hàng");
    }

    List<ProductImage> productImages = images.findByProductIdInOrderByDisplayOrderAscIdAsc(List.of(productId));
    images.deleteAll(productImages);
    productImages.forEach(image -> fileStorage.delete(image.getImagePath()));
    products.delete(product);
  }

  @Override
  @Transactional
  public ProductRemovalResult deleteOrStopSelling(Long productId) {
    if (orderReferences.hasOrders(productId)) {
      Product product = find(productId);
      product.setStatus(ProductStatus.STOPPED);
      return ProductRemovalResult.STOPPED;
    } else {
      deleteProduct(productId);
      return ProductRemovalResult.DELETED;
    }
  }

  private void copy(Product product, String name, String description, java.math.BigDecimal price,
      Integer stock, Integer preparationDays, ProductStatus status, Long categoryId) {
    product.setName(name.trim());
    product.setDescription(description);
    product.setPrice(price);
    product.setStockQuantity(stock);
    product.setPreparationDays(preparationDays);
    product.setStatus(status);
    product.setCategory(categories.findById(categoryId)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục")));
  }

  private void saveImages(Product product, List<MultipartFile> files, List<String> storedPaths) {
    if (files == null) return;
    int order = images.findByProductIdInOrderByDisplayOrderAscIdAsc(List.of(product.getId())).size();
    for (MultipartFile file : files) {
      if (file == null || file.isEmpty() || !StringUtils.hasText(file.getOriginalFilename())) continue;
      try {
        ProductImage image = new ProductImage();
        image.setProduct(product);
        image.setImagePath(fileStorage.store(file));
        storedPaths.add(image.getImagePath());
        image.setDisplayOrder(order++);
        images.save(image);
      } catch (RuntimeException exception) {
        throw new IllegalStateException("Không thể lưu hình ảnh sản phẩm", exception);
      }
    }
  }

  private List<MultipartFile> validateImages(List<MultipartFile> files, int existingCount,
      boolean required) {
    List<MultipartFile> validFiles = files == null ? List.of() : files.stream()
        .filter(file -> file != null && !file.isEmpty())
        .toList();
    if (required && (validFiles.isEmpty() || validFiles.size() > MAX_IMAGE_COUNT)) {
      throw new IllegalArgumentException("Sản phẩm cần từ 1 đến 4 hình ảnh thực sự hợp lệ");
    }
    if (!required && existingCount + validFiles.size() > MAX_IMAGE_COUNT) {
      throw new IllegalArgumentException("Tổng số hình ảnh sản phẩm không được vượt quá 4");
    }
    for (MultipartFile file : validFiles) {
      if (!StringUtils.hasText(file.getOriginalFilename())
          || file.getSize() > MAX_IMAGE_SIZE_BYTES
          || file.getContentType() == null
          || !file.getContentType().toLowerCase().startsWith("image/")
          || isNotImage(file)) {
        throw new IllegalArgumentException("Tệp tải lên phải là hình ảnh không vượt quá 5 MB");
      }
    }
    return validFiles;
  }

  private boolean isNotImage(MultipartFile file) {
    try {
      return ImageIO.read(file.getInputStream()) == null;
    } catch (java.io.IOException exception) {
      throw new IllegalArgumentException("Không thể đọc tệp hình ảnh", exception);
    }
  }

  private void deleteStoredFiles(List<String> storedPaths, RuntimeException originalException) {
    for (String path : storedPaths) {
      try {
        fileStorage.delete(path);
      } catch (RuntimeException cleanupException) {
        originalException.addSuppressed(cleanupException);
      }
    }
  }

  private Product find(Long id) {
    return products.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
  }

  private ProductSummaryResponse summary(Product product, List<String> imagePaths,
      List<Long> imageIds) {
    ProductSummaryResponse response = new ProductSummaryResponse();
    response.setId(product.getId());
    response.setName(product.getName());
    response.setDescription(product.getDescription());
    response.setImagePaths(imagePaths);
    response.setImageIds(imageIds);
    response.setPrimaryImagePath(imagePaths.isEmpty() ? null : imagePaths.get(0));
    response.setPrice(product.getPrice());
    response.setStockQuantity(product.getStockQuantity());
    response.setPreparationDays(product.getPreparationDays());
    response.setStatus(product.getStatus());
    response.setCategoryId(product.getCategory().getId());
    response.setCategoryName(product.getCategory().getName());
    return response;
  }
}
