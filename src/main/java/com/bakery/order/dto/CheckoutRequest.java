package com.bakery.order.dto;

import static com.bakery.common.validation.ValidationMessages.INVALID_EMAIL;
import static com.bakery.common.validation.ValidationMessages.INVALID_PHONE;
import static com.bakery.common.validation.ValidationMessages.REQUIRED;

import com.bakery.common.validation.ValidationPatterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CheckoutRequest {

  @NotBlank(message = REQUIRED)
  @Size(max = 100)
  private String receiverName;

  @NotBlank(message = REQUIRED)
  @Pattern(regexp = ValidationPatterns.PHONE, message = INVALID_PHONE)
  private String receiverPhone;

  @NotBlank(message = REQUIRED)
  @Size(max = 500)
  private String receiverAddress;

  @Email(message = INVALID_EMAIL)
  @Size(max = 150)
  private String receiverEmail;

  @NotNull(message = "Vui lòng chọn ngày nhận bánh")
  @FutureOrPresent(message = "Ngày nhận bánh không được nằm trong quá khứ")
  private LocalDate deliveryDate;

  private LocalTime deliveryTime;

  @Size(max = 1000)
  private String note;

  private Map<Long, String> productNotes = new HashMap<>();
}
