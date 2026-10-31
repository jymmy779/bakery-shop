package com.bakery.catalog.service.impl;

import com.bakery.catalog.dto.ProductCreateRequest;
import com.bakery.catalog.dto.ProductDetailResponse;
import com.bakery.catalog.dto.ProductSummaryResponse;
import com.bakery.catalog.dto.ProductUpdateRequest;
import com.bakery.catalog.entity.Product;
import com.bakery.catalog.entity.ProductBadge;
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
import java.util.Map;
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
        request.getCategoryId(), request.getBadge(), request.getIngredients(),
        request.getStorageInstructions(), request.getAllergenInfo());
    List<String> storedPaths = new ArrayList<>();
    try {
      product = products.save(product);
      saveImages(product, validImages, storedPaths, request.getImageOrder(), List.of());
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
    List<ProductImage> existingImages = images.findByProductIdInOrderByDisplayOrderAscIdAsc(
        List.of(productId));
    int existingImageCount = existingImages.size();
    List<MultipartFile> validImages = validateImages(request.getNewImages(), existingImageCount, false);
    copy(product, request.getName(), request.getDescription(), request.getPrice(),
        request.getStockQuantity(), request.getPreparationDays(), request.getStatus(),
        request.getCategoryId(), request.getBadge(), request.getIngredients(),
        request.getStorageInstructions(), request.getAllergenInfo());
    List<String> storedPaths = new ArrayList<>();
    try {
      products.save(product);
      saveImages(product, validImages, storedPaths, request.getImageOrder(), existingImages);
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
      ProductStatus status, String sort, Pageable pageable) {
    return search(keyword, categoryId, null, status, sort, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ProductSummaryResponse> search(String keyword, Long categoryId,
      ProductBadge badge, ProductStatus status, String sort, Pageable pageable) {
    if (sort == null
        || !java.util.Set.of("newest", "oldest", "price_asc", "price_desc", "name").contains(sort)) {
      throw new IllegalArgumentException(
          "sort phải là newest, oldest, price_asc, price_desc hoặc name");
    }
    String normalized = keyword == null || keyword.isBlank() ? null : keyword.trim();
    Page<Product> page = products.searchAdmin(normalized, categoryId, badge, status, sort, pageable);
    List<Product> content = page.getContent();
    var primaryImages = new HashMap<Long, String>();
    if (!content.isEmpty()) {
      images.findByProductIdInOrderByDisplayOrderAscIdAsc(
          content.stream().map(Product::getId).toList())
          .forEach(image -> primaryImages.putIfAbsent(
              image.getProduct().getId(), image.getImagePath()));
    }
    List<ProductSummaryResponse> summaries = content.stream()
        .map(product -> summary(product, primaryImages.get(product.getId()))).toList();
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

  @Override
  @Transactional
  public void deleteAllImages(Long productId) {
    find(productId);
    List<ProductImage> productImages =
        images.findByProductIdInOrderByDisplayOrderAscIdAsc(List.of(productId));
    images.deleteAll(productImages);
    productImages.forEach(image -> fileStorage.delete(image.getImagePath()));
  }

  @Transactional
  public void deleteProduct(Long productId) {
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
      Integer stock, Integer preparationDays, ProductStatus status, Long categoryId,
      ProductBadge badge, String ingredients, String storageInstructions, String allergenInfo) {
    product.setName(name.trim());
    product.setDescription(description);
    product.setBadge(badge);
    product.setIngredients(trimToNull(ingredients));
    product.setStorageInstructions(trimToNull(storageInstructions));
    product.setAllergenInfo(trimToNull(allergenInfo));
    product.setPrice(price);
    product.setStockQuantity(stock);
    product.setPreparationDays(preparationDays);
    product.setStatus(status);
    product.setCategory(categories.findById(categoryId)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục")));
  }

  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  private void saveImages(Product product, List<MultipartFile> files, List<String> storedPaths,
      String requestedOrder, List<ProductImage> existing) {
    List<String> order = resolveImageOrder(requestedOrder, existing, files.size());
    if (existing.isEmpty() && files.isEmpty()) return;

    if (!existing.isEmpty()) {
      int temporaryOffset = existing.stream().mapToInt(ProductImage::getDisplayOrder).max().orElse(0)
          + existing.size() + files.size() + MAX_IMAGE_COUNT + 1;
      existing.forEach(image -> image.setDisplayOrder(image.getDisplayOrder() + temporaryOffset));
      images.saveAllAndFlush(existing);
    }

    Map<Long, ProductImage> existingById = existing.stream()
        .collect(java.util.stream.Collectors.toMap(ProductImage::getId, image -> image));
    List<ProductImage> newImages = new ArrayList<>();
    int position = 0;
    for (String token : order) {
      if (token.startsWith("existing:")) {
        ProductImage image = existingById.get(Long.parseLong(token.substring("existing:".length())));
        image.setDisplayOrder(position++);
        continue;
      }
      int fileIndex = Integer.parseInt(token.substring("new:".length()));
      MultipartFile file = files.get(fileIndex);
      try {
        ProductImage image = new ProductImage();
        image.setProduct(product);
        image.setImagePath(fileStorage.store(file));
        storedPaths.add(image.getImagePath());
        image.setDisplayOrder(position++);
        newImages.add(image);
      } catch (RuntimeException exception) {
        throw new IllegalStateException("Không thể lưu hình ảnh sản phẩm", exception);
      }
    }
    if (!newImages.isEmpty()) images.saveAll(newImages);
    if (!existing.isEmpty()) images.saveAllAndFlush(existing);
  }

  private List<String> resolveImageOrder(String requestedOrder, List<ProductImage> existing,
      int newFileCount) {
    List<String> expected = new ArrayList<>();
    existing.forEach(image -> expected.add("existing:" + image.getId()));
    for (int index = 0; index < newFileCount; index++) expected.add("new:" + index);
    if (requestedOrder == null || requestedOrder.isBlank()) return expected;

    List<String> supplied = java.util.Arrays.stream(requestedOrder.split(","))
        .map(String::trim).filter(token -> !token.isEmpty()).toList();
    if (supplied.size() != expected.size()
        || !new java.util.HashSet<>(supplied).equals(new java.util.HashSet<>(expected))) {
      throw new IllegalArgumentException("Thứ tự ảnh không hợp lệ. Vui lòng tải lại biểu mẫu.");
    }
    return supplied;
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

  private ProductSummaryResponse summary(Product product, String primaryImagePath) {
    ProductSummaryResponse response = new ProductSummaryResponse();
    response.setId(product.getId());
    response.setName(product.getName());
    response.setDescription(product.getDescription());
    response.setBadge(product.getBadge());
    response.setIngredients(product.getIngredients());
    response.setStorageInstructions(product.getStorageInstructions());
    response.setAllergenInfo(product.getAllergenInfo());
    response.setPrimaryImagePath(primaryImagePath);
    response.setPrice(product.getPrice());
    response.setStockQuantity(product.getStockQuantity());
    response.setPreparationDays(product.getPreparationDays());
    response.setStatus(product.getStatus());
    response.setCategoryId(product.getCategory().getId());
    response.setCategoryName(product.getCategory().getName());
    return response;
  }
}
