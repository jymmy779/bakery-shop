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
import com.bakery.catalog.service.CatalogQueryService;
import com.bakery.catalog.service.ProductService;
import com.bakery.order.service.ProductOrderReferenceService;
import com.bakery.storage.FileStorageService;
import java.util.List;
import java.util.HashMap;
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
public class ProductServiceImpl implements ProductService {
  private final ProductRepository products;
  private final ProductImageRepository images;
  private final CategoryRepository categories;
  private final ProductOrderReferenceService orderReferences;
  private final CatalogQueryService catalog;
  private final FileStorageService fileStorage;

  @Override
  @Transactional
  public ProductDetailResponse create(ProductCreateRequest request) {
    Product product = new Product();
    copy(product, request.getName(), request.getDescription(), request.getPrice(),
        request.getStockQuantity(), request.getPreparationDays(), ProductStatus.SELLING,
        request.getCategoryId());
    product = products.save(product);
    saveImages(product, request.getImages());
    return catalog.getById(product.getId());
  }

  @Override
  @Transactional
  public ProductDetailResponse update(Long productId, ProductUpdateRequest request) {
    Product product = find(productId);
    copy(product, request.getName(), request.getDescription(), request.getPrice(),
        request.getStockQuantity(), request.getPreparationDays(), request.getStatus(),
        request.getCategoryId());
    products.save(product);
    saveImages(product, request.getNewImages());
    return catalog.getById(productId);
  }

  @Override
  @Transactional(readOnly = true)
  public ProductDetailResponse getById(Long productId) {
    return catalog.getById(productId);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ProductSummaryResponse> search(String keyword, Long categoryId,
      ProductStatus status, Pageable pageable) {
    String normalized = keyword == null || keyword.isBlank() ? null : keyword.trim();
    Page<Product> page = products.searchAdmin(normalized, categoryId, status, pageable);
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
  public void stopSellingProduct(Long productId) {
    Product product = find(productId);
    product.setStatus(ProductStatus.STOPPED);
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
  public void deleteOrStopSelling(Long productId) {
    if (orderReferences.hasOrders(productId)) {
      stopSellingProduct(productId);
    } else {
      deleteProduct(productId);
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

  private void saveImages(Product product, List<MultipartFile> files) {
    if (files == null) return;
    int order = images.findByProductIdInOrderByDisplayOrderAscIdAsc(List.of(product.getId())).size();
    for (MultipartFile file : files) {
      if (file == null || file.isEmpty() || !StringUtils.hasText(file.getOriginalFilename())) continue;
      try {
        ProductImage image = new ProductImage();
        image.setProduct(product);
        image.setImagePath(fileStorage.store(file));
        image.setDisplayOrder(order++);
        images.save(image);
      } catch (RuntimeException exception) {
        throw new IllegalStateException("Không thể lưu hình ảnh sản phẩm", exception);
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