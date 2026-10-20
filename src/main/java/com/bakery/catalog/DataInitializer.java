package com.bakery.catalog;

import com.bakery.catalog.entity.Category;
import com.bakery.catalog.entity.Product;
import com.bakery.catalog.entity.ProductImage;
import com.bakery.catalog.entity.ProductStatus;
import com.bakery.catalog.repository.CategoryRepository;
import com.bakery.catalog.repository.ProductImageRepository;
import com.bakery.catalog.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
  private final CategoryRepository categories;
  private final ProductRepository products;
  private final ProductImageRepository images;

  @Override
  public void run(String... args) {
    if (categories.count() == 0) {
      categories.saveAll(List.of(category("Bánh kem", "Bánh kem tươi cho những dịp đặc biệt"),
          category("Bánh mì", "Bánh mì nướng mới mỗi ngày"),
          category("Bánh quy", "Bánh quy giòn thơm để sẻ chia"),
          category("Bánh ngọt", "Những món bánh ngọt dịu dàng"),
          category("Cookies", "Cookies bơ thủ công")));
    }
    if (products.count() == 0) {
      List<Category> categoryList = categories.findAll();
      addProduct("Bánh kem dâu tây", "Kem tươi và dâu tây mọng trên cốt bánh vani.", "185000", 12, categoryList.get(0));
      addProduct("Bánh kem chocolate", "Cốt chocolate đậm vị cùng lớp ganache mềm mịn.", "210000", 8, categoryList.get(0));
      addProduct("Bánh mì bơ tỏi", "Bánh mì vàng giòn, thơm bơ tỏi và mùi thảo mộc.", "35000", 30, categoryList.get(1));
      addProduct("Bánh mì sữa", "Bánh mì sữa mềm xốp, vị ngọt vừa phải.", "28000", 25, categoryList.get(1));
      addProduct("Bánh quy bơ", "Bánh quy bơ tan nhẹ, nướng theo mẻ nhỏ.", "65000", 20, categoryList.get(2));
      addProduct("Bánh quy hạnh nhân", "Hạnh nhân rang giòn trong lớp bánh quy bơ.", "75000", 16, categoryList.get(2));
      addProduct("Croissant bơ", "Croissant nhiều lớp, nở xốp và thơm mùi bơ.", "42000", 18, categoryList.get(3));
      addProduct("Tart trứng", "Đế tart giòn với nhân trứng sữa béo nhẹ.", "38000", 14, categoryList.get(3));
      addProduct("Cookies chocolate chip", "Cookies giòn ngoài, mềm trong với chocolate chip.", "55000", 22, categoryList.get(4));
      addProduct("Madeleine chanh", "Madeleine nhỏ xinh, thơm vỏ chanh tươi.", "48000", 20, categoryList.get(4));
    }
  }

  private Category category(String name, String description) {
    Category category = new Category();
    category.setName(name);
    category.setDescription(description);
    category.setActive(true);
    return category;
  }

  private void addProduct(String name, String description, String price, int stock, Category category) {
    Product product = new Product();
    product.setName(name);
    product.setDescription(description);
    product.setPrice(new BigDecimal(price));
    product.setStockQuantity(stock);
    product.setPreparationDays(1);
    product.setStatus(ProductStatus.SELLING);
    product.setCategory(category);
    product = products.save(product);

    ProductImage image = new ProductImage();
    image.setProduct(product);
    image.setImagePath("/assets/product-placeholder.svg");
    image.setDisplayOrder(0);
    images.save(image);
  }
}