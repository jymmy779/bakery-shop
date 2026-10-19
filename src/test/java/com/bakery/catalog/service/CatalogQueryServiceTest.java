package com.bakery.catalog.service;

import com.bakery.catalog.entity.Category;
import com.bakery.catalog.entity.Product;
import com.bakery.catalog.entity.ProductImage;
import com.bakery.catalog.repository.ProductImageRepository;
import com.bakery.catalog.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import com.bakery.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CatalogQueryServiceTest {
  private final ProductRepository products = mock(ProductRepository.class);
  private final ProductImageRepository images = mock(ProductImageRepository.class);
  private final CatalogQueryService service = new CatalogQueryService(products, images);

  @Test
  void combinesFiltersAndKeepsDatabasePageTotal() {
    var pageable = PageRequest.of(1, 2);
    var min = new BigDecimal("25000");
    var max = new BigDecimal("300000");
    when(products.filter("%dâu%", 1L, min, max, "best_selling", pageable))
        .thenReturn(new PageImpl<>(List.of(), pageable, 5));
    var result = service.filter("  dâu  ", 1L, min, max, "best_selling", 1, 2);
    assertEquals(5, result.getTotalElements());
    assertEquals(3, result.getTotalPages());
    verify(products).filter("%dâu%", 1L, min, max, "best_selling", pageable);
  }

  @Test
  void rejectsInvalidPricePaginationAndSortBeforeQuerying() {
    assertThrows(IllegalArgumentException.class, () ->
        service.filter("", null, new BigDecimal("10"), BigDecimal.ONE, "name", 0, 12));
    assertThrows(IllegalArgumentException.class, () ->
        service.filter("", null, BigDecimal.valueOf(-1), null, "name", 0, 12));
    assertThrows(IllegalArgumentException.class, () ->
        service.filter("", null, null, null, "bad_sort", 0, 12));
    assertThrows(IllegalArgumentException.class, () ->
        service.filter("", null, null, null, "name", -1, 12));
    assertThrows(IllegalArgumentException.class, () ->
        service.filter("", null, null, null, "name", 0, 101));
    verifyNoInteractions(products, images);
  }

  @Test
  void searchesDatabaseUsingTrimmedKeyword() {
    Category category = new Category();
    category.setName("Cake");
    Product product = new Product();
    product.setId(10L);
    product.setName("Tiramisu");
    product.setCategory(category);
    when(products.searchByNameOrDescription("%TIRA%"))
        .thenReturn(List.of(product));
    when(images.findByProductIdInOrderByDisplayOrderAscIdAsc(List.of(10L))).thenReturn(List.of());

    var result = service.list("  TIRA  ");

    assertEquals(1, result.size());
    assertEquals("Tiramisu", result.get(0).getName());
    verify(products, never()).findAll(any(Sort.class));
  }

  @Test
  void blankKeywordReturnsAllProducts() {
    when(products.findAll(any(Sort.class))).thenReturn(List.of());
    assertTrue(service.list("   ").isEmpty());
    verify(products).findAll(Sort.by("name", "id"));
    verify(products, never()).searchByNameOrDescription(anyString());
  }

  @Test
  void noSearchMatchesReturnsEmptyList() {
    when(products.searchByNameOrDescription("%missing%"))
        .thenReturn(List.of());
    assertTrue(service.list("missing").isEmpty());
    verifyNoInteractions(images);
  }

  @Test
  void treatsSqlWildcardsAsLiteralCharacters() {
    when(products.searchByNameOrDescription("%50!%!_!!%")).thenReturn(List.of());
    assertTrue(service.list("50%_!").isEmpty());
    verify(products).searchByNameOrDescription("%50!%!_!!%");
  }

  @Test
  void normalizesVietnameseDInBothCases() {
    when(products.searchByNameOrDescription("%Dậu do%")).thenReturn(List.of());
    service.list("Đậu đo");
    verify(products).searchByNameOrDescription("%Dậu do%");
  }

  @Test
  void detailReturnsDatabaseDescriptionStockAndImages() {
    Category category = new Category();
    category.setId(1L);
    category.setName("Cake");
    Product product = new Product();
    product.setId(10L);
    product.setName("Tiramisu");
    product.setDescription("Coffee and mascarpone");
    product.setPrice(new BigDecimal("65000"));
    product.setStockQuantity(15);
    product.setPreparationDays(1);
    product.setCategory(category);
    ProductImage image = new ProductImage();
    image.setId(2L);
    image.setImagePath("/uploads/tiramisu.jpg");
    when(products.findById(10L)).thenReturn(Optional.of(product));
    when(images.findByProductIdInOrderByDisplayOrderAscIdAsc(List.of(10L))).thenReturn(List.of(image));

    var result = service.getById(10L);

    assertEquals("Tiramisu", result.getName());
    assertEquals("Coffee and mascarpone", result.getDescription());
    assertEquals(product.getPrice(), result.getPrice());
    assertEquals(15, result.getStockQuantity());
    assertEquals(1, result.getPreparationDays());
    assertEquals("Cake", result.getCategory().getName());
    assertEquals("/uploads/tiramisu.jpg", result.getImages().get(0).getImagePath());
  }

  @Test
  void missingCakeReturnsNotFoundInsteadOfNullDetail() {
    when(products.findById(999L)).thenReturn(Optional.empty());
    assertThrows(ResourceNotFoundException.class, () -> service.getById(999L));
    verifyNoInteractions(images);
  }

  @Test
  void mapsDatabaseProductsWithCategoryAndFirstImage() {
    Category category = new Category();
    category.setId(1L);
    category.setName("Bánh kem");
    Product product = new Product();
    product.setId(10L);
    product.setName("Bánh dâu");
    product.setCategory(category);
    product.setPrice(new BigDecimal("150000"));
    ProductImage first = new ProductImage();
    first.setProduct(product);
    first.setImagePath("/uploads/first.jpg");
    ProductImage second = new ProductImage();
    second.setProduct(product);
    second.setImagePath("/uploads/second.jpg");
    when(products.findAll(any(Sort.class)))
        .thenReturn(List.of(product));
    when(images.findByProductIdInOrderByDisplayOrderAscIdAsc(List.of(10L)))
        .thenReturn(List.of(first, second));

    var result = service.list();

    assertEquals(1, result.size());

    assertEquals("Bánh kem", result.get(0).getCategoryName());
    assertEquals("/uploads/first.jpg", result.get(0).getPrimaryImagePath());
    assertEquals(product.getPrice(), result.get(0).getPrice());
  }

  @Test
  void emptyDatabaseDoesNotQueryImages() {
    when(products.findAll(any(Sort.class)))
        .thenReturn(List.of());

    assertTrue(service.list().isEmpty());
    verifyNoInteractions(images);
  }
}

