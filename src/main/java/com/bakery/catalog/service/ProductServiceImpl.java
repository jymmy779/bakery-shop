package com.bakery.catalog.service;

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
import com.bakery.order.repository.OrderDetailRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService, CatalogAdminProductService {
  private final ProductRepository products;
  private final ProductImageRepository images;
  private final CategoryRepository categories;
  private final OrderDetailRepository orderDetails;
  private final CatalogQueryService catalog;

  @Value("${app.upload-dir:uploads}")
  private String uploadDirectory;

  @Override
  @Transactional
  public ProductDetailResponse createProduct(ProductCreateRequest request) {
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
  public ProductDetailResponse updateProduct(Long productId, ProductUpdateRequest request) {
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
  public Page<ProductSummaryResponse> getAllProducts(String keyword, Long categoryId,
      ProductStatus status, Pageable pageable) {
    String normalized = keyword == null || keyword.isBlank() ? null : keyword.trim();
    Page<Product> page = products.searchAdmin(normalized, categoryId, status, pageable);
    List<ProductSummaryResponse> summaries = page.getContent().stream().map(this::summary).toList();
    return new PageImpl<>(summaries, page.getPageable(), page.getTotalElements());
  }

  @Override
  @Transactional
  public void stopSellingProduct(Long productId) {
    Product product = find(productId);
    product.setStatus(ProductStatus.STOPPED);
  }

  @Override
  @Transactional
  public void deleteProduct(Long productId) {
    Product product = find(productId);
    if (orderDetails.findAll().stream()
        .anyMatch(orderDetail -> orderDetail.getProduct().getId().equals(productId))) {
      throw new IllegalStateException(
          "Sản phẩm đã từng có trong đơn hàng, hãy dùng chức năng Ngừng bán để bảo toàn lịch sử mua hàng");
    }

    List<ProductImage> productImages = images.findByProductIdInOrderByDisplayOrderAscIdAsc(List.of(productId));
    images.deleteAll(productImages);
    productImages.forEach(image -> deleteFile(image.getImagePath()));
    products.delete(product);
  }

  @Override
  @Transactional
  public ProductDetailResponse create(ProductCreateRequest request) {
    return createProduct(request);
  }

  @Override
  @Transactional
  public ProductDetailResponse update(Long productId, ProductUpdateRequest request) {
    return updateProduct(productId, request);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ProductSummaryResponse> search(String keyword, Long categoryId,
      ProductStatus status, Pageable pageable) {
    return getAllProducts(keyword, categoryId, status, pageable);
  }

  @Override
  @Transactional
  public void deleteOrStopSelling(Long productId) {
    if (orderDetails.findAll().stream()
        .anyMatch(orderDetail -> orderDetail.getProduct().getId().equals(productId))) {
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
        Path directory = Path.of(uploadDirectory).toAbsolutePath().normalize();
        Files.createDirectories(directory);
        String filename = product.getId() + "-" + java.util.UUID.randomUUID() + "-"
            + Path.of(file.getOriginalFilename()).getFileName();
        Files.copy(file.getInputStream(), directory.resolve(filename));
        ProductImage image = new ProductImage();
        image.setProduct(product);
        image.setImagePath("/uploads/" + filename);
        image.setDisplayOrder(order++);
        images.save(image);
      } catch (IOException exception) {
        throw new IllegalStateException("Không thể lưu hình ảnh sản phẩm", exception);
      }
    }
  }

  private void deleteFile(String imagePath) {
    if (imagePath == null || !imagePath.startsWith("/uploads/")) return;
    try {
      Files.deleteIfExists(Path.of(uploadDirectory).toAbsolutePath().normalize()
          .resolve(imagePath.substring("/uploads/".length())).normalize());
    } catch (IOException ignored) {
      // The database operation remains valid even when an old file is already missing.
    }
  }

  private Product find(Long id) {
    return products.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
  }

  private ProductSummaryResponse summary(Product product) {
    ProductSummaryResponse response = new ProductSummaryResponse();
    response.setId(product.getId());
    response.setName(product.getName());
    response.setPrice(product.getPrice());
    response.setStockQuantity(product.getStockQuantity());
    response.setPreparationDays(product.getPreparationDays());
    response.setStatus(product.getStatus());
    response.setCategoryId(product.getCategory().getId());
    response.setCategoryName(product.getCategory().getName());
    return response;
  }
}