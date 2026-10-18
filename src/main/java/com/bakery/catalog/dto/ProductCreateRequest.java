package com.bakery.catalog.dto;

import static com.bakery.common.validation.ValidationMessages.NON_NEGATIVE_STOCK;
import static com.bakery.common.validation.ValidationMessages.POSITIVE_PRICE;
import static com.bakery.common.validation.ValidationMessages.REQUIRED;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class ProductCreateRequest {

  @NotBlank(message = REQUIRED)
  @Size(max = 150)
  private String name;

  @NotBlank(message = REQUIRED)
  private String description;

  @NotNull(message = REQUIRED)
  @DecimalMin(value = "0.01", message = POSITIVE_PRICE)
  private BigDecimal price;

  @NotNull(message = REQUIRED)
  @PositiveOrZero(message = NON_NEGATIVE_STOCK)
  private Integer stockQuantity;

  @NotNull(message = REQUIRED)
  @PositiveOrZero(message = "Số ngày chuẩn bị không được âm")
  private Integer preparationDays;

  @NotNull(message = "Vui lòng chọn danh mục")
  private Long categoryId;

  @Size(min = 1, max = 4, message = "Sản phẩm cần từ 1 đến 4 hình ảnh")
  private List<MultipartFile> images = new ArrayList<>();
}
