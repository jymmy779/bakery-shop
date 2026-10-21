package com.bakery.catalog.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bakery.catalog.dto.ProductCreateRequest;
import com.bakery.catalog.dto.ProductUpdateRequest;
import com.bakery.catalog.entity.Category;
import com.bakery.catalog.entity.Product;
import com.bakery.catalog.entity.ProductStatus;
import com.bakery.catalog.repository.CategoryRepository;
import com.bakery.catalog.repository.ProductImageRepository;
import com.bakery.catalog.repository.ProductRepository;
import com.bakery.catalog.service.CatalogQueryService;
import com.bakery.order.service.ProductOrderReferenceService;
import com.bakery.storage.FileStorageService;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;
import org.springframework.mock.web.MockMultipartFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

  @Mock ProductRepository products;
  @Mock ProductImageRepository images;
  @Mock CategoryRepository categories;
  @Mock ProductOrderReferenceService orderReferences;
  @Mock FileStorageService fileStorage;
  @Mock CatalogQueryService catalog;

  private ProductServiceImpl service;
  private Category category;

  @BeforeEach
  void setUp() {
    service = new ProductServiceImpl(products, images, categories, orderReferences, catalog,
        fileStorage);
    category = new Category();
    category.setId(1L);
    category.setName("Bánh");
  }

  @Test
  void createsProductWithSellingStatus() {
    ProductCreateRequest request = new ProductCreateRequest();
    request.setName("Bánh mới");
    request.setDescription("Mô tả");
    request.setPrice(new BigDecimal("25000"));
    request.setStockQuantity(10);
    request.setPreparationDays(1);
    request.setCategoryId(1L);
    request.setImages(List.of(validImage()));
    when(categories.findById(1L)).thenReturn(Optional.of(category));
    Product saved = new Product();
    saved.setId(5L);
    when(products.save(any(Product.class))).thenReturn(saved);

    service.create(request);

    verify(products).save(any(Product.class));
    verify(catalog).getById(5L);
  }

  @Test
  void updatesProductStatusAndFields() {
    Product product = product(5L);
    when(products.findById(5L)).thenReturn(Optional.of(product));
    when(categories.findById(1L)).thenReturn(Optional.of(category));
    ProductUpdateRequest request = updateRequest(ProductStatus.STOPPED);

    service.update(5L, request);

    assertEquals("Bánh sửa", product.getName());
    assertEquals(ProductStatus.STOPPED, product.getStatus());
    verify(products).save(product);
  }

  @Test
  void stopsSellingProduct() {
    Product product = product(5L);
    when(products.findById(5L)).thenReturn(Optional.of(product));

    service.stopSellingProduct(5L);

    assertEquals(ProductStatus.STOPPED, product.getStatus());
  }

  @Test
  void doesNotDeleteProductReferencedByOrder() {
    Product product = product(5L);
    when(products.findById(5L)).thenReturn(Optional.of(product));
    when(orderReferences.hasOrders(5L)).thenReturn(true);

    assertEquals(com.bakery.catalog.service.ProductRemovalResult.STOPPED,
        service.deleteOrStopSelling(5L));

    assertEquals(ProductStatus.STOPPED, product.getStatus());
    verify(products, org.mockito.Mockito.never()).delete(any(Product.class));
  }

  @Test
  void deletesProductWithoutOrders() {
    Product product = product(5L);
    when(products.findById(5L)).thenReturn(Optional.of(product));
    when(orderReferences.hasOrders(5L)).thenReturn(false);
    when(images.findByProductIdInOrderByDisplayOrderAscIdAsc(java.util.List.of(5L)))
        .thenReturn(java.util.List.of());

    assertEquals(com.bakery.catalog.service.ProductRemovalResult.DELETED,
        service.deleteOrStopSelling(5L));

    verify(products).delete(product);
  }

  @Test
  void togglesProductStatusInBothDirections() {
    Product product = product(5L);
    when(products.findById(5L)).thenReturn(Optional.of(product));

    service.toggleStatus(5L);
    assertEquals(ProductStatus.STOPPED, product.getStatus());

    service.toggleStatus(5L);
    assertEquals(ProductStatus.SELLING, product.getStatus());
  }

  @Test
  void rejectsCreateWithoutRealImages() {
    ProductCreateRequest request = createRequest();
    request.setImages(List.of(new MockMultipartFile("images", "empty.png", "image/png", new byte[0])));

    assertThrows(IllegalArgumentException.class, () -> service.create(request));
  }

  @Test
  void rejectsCreateWithMoreThanFourImages() {
    ProductCreateRequest request = createRequest();
    request.setImages(List.of(validImage(), validImage(), validImage(), validImage(), validImage()));

    assertThrows(IllegalArgumentException.class, () -> service.create(request));
  }

  @Test
  void rejectsUpdateWhenTotalImagesExceedFour() {
    Product product = product(5L);
    when(products.findById(5L)).thenReturn(Optional.of(product));
    when(images.findByProductIdInOrderByDisplayOrderAscIdAsc(List.of(5L)))
        .thenReturn(List.of(new com.bakery.catalog.entity.ProductImage(),
            new com.bakery.catalog.entity.ProductImage(),
            new com.bakery.catalog.entity.ProductImage()));
    ProductUpdateRequest request = updateRequest(ProductStatus.SELLING);
    request.setNewImages(List.of(validImage(), validImage()));

    assertThrows(IllegalArgumentException.class, () -> service.update(5L, request));
  }

  @Test
  void rejectsNonImageFile() {
    ProductCreateRequest request = createRequest();
    request.setImages(List.of(new MockMultipartFile("images", "notes.txt", "image/png",
        "not an image".getBytes())));

    assertThrows(IllegalArgumentException.class, () -> service.create(request));
  }

  @Test
  void rejectsImageWithoutFilename() {
    ProductCreateRequest request = createRequest();
    request.setImages(List.of(new MockMultipartFile("images", "", "image/png",
        new byte[] {1, 2, 3})));

    assertThrows(IllegalArgumentException.class, () -> service.create(request));
  }

  private ProductCreateRequest createRequest() {
    ProductCreateRequest request = new ProductCreateRequest();
    request.setName("Bánh mới");
    request.setDescription("Mô tả");
    request.setPrice(new BigDecimal("25000"));
    request.setStockQuantity(10);
    request.setPreparationDays(1);
    request.setCategoryId(1L);
    return request;
  }

  private MockMultipartFile validImage() {
    return new MockMultipartFile("images", "cake.png", "image/png",
        java.util.Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="));
  }

  @Test
  void passesKeywordCategoryAndStatusToAdminFilter() {
    var pageable = org.springframework.data.domain.PageRequest.of(0, 10);
    when(products.searchAdmin("cake", 1L, ProductStatus.SELLING, pageable))
        .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(), pageable, 0));

    var result = service.search(" cake ", 1L, ProductStatus.SELLING, pageable);

    assertEquals(0, result.getTotalElements());
    verify(products).searchAdmin("cake", 1L, ProductStatus.SELLING, pageable);
  }

  private ProductUpdateRequest updateRequest(ProductStatus status) {
    ProductUpdateRequest request = new ProductUpdateRequest();
    request.setName("Bánh sửa");
    request.setDescription("Mô tả sửa");
    request.setPrice(new BigDecimal("30000"));
    request.setStockQuantity(8);
    request.setPreparationDays(2);
    request.setCategoryId(1L);
    request.setStatus(status);
    return request;
  }

  private Product product(Long id) {
    Product product = new Product();
    product.setId(id);
    product.setName("Bánh cũ");
    product.setDescription("Mô tả");
    product.setPrice(new BigDecimal("20000"));
    product.setStockQuantity(3);
    product.setPreparationDays(1);
    product.setStatus(ProductStatus.SELLING);
    product.setCategory(category);
    return product;
  }
}
