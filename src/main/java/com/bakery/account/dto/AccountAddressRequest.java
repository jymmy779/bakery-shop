package com.bakery.account.dto;

import com.bakery.common.validation.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AccountAddressRequest {

  @NotBlank(message = "Vui lòng nhập tên gợi nhớ")
  @Size(max = 80)
  private String label;

  @NotBlank(message = "Vui lòng nhập tên người nhận")
  @Size(max = 100)
  private String receiverName;

  @NotBlank(message = "Vui lòng nhập số điện thoại")
  @Pattern(regexp = ValidationPatterns.PHONE, message = "Số điện thoại không hợp lệ")
  private String receiverPhone;

  @NotBlank(message = "Vui lòng nhập địa chỉ")
  @Size(max = 500)
  private String address;

  @Size(max = 1000)
  private String deliveryNote;

  private boolean defaultAddress;
}
