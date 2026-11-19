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
import jakarta.validation.constraints.Positive;
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

  @NotBlank(message = "Vui lòng mở lại trang thanh toán trước khi đặt hàng")
  @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
      message = "Phiên đặt hàng không hợp lệ, vui lòng mở lại trang thanh toán")
  private String checkoutToken;

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

  @Size(max = 200, message = "Lời nhắn không được vượt quá 200 ký tự")
  @Pattern(regexp = "(?s)^(?!.*<\\s*(?:/?\\s*[a-zA-Z]|[!?])).*$",
      message = "Lời nhắn chỉ nhận văn bản, vui lòng bỏ thẻ HTML")
  private String note;

  private Map<@NotNull @Positive Long,
      @Size(max = 200, message = "Lời nhắn cho bánh không được vượt quá 200 ký tự")
      @Pattern(regexp = "(?s)^(?!.*<\\s*(?:/?\\s*[a-zA-Z]|[!?])).*$",
          message = "Lời nhắn cho bánh chỉ nhận văn bản") String> productNotes = new HashMap<>();
}
