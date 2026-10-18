package com.bakery.account.dto;

import static com.bakery.common.validation.ValidationMessages.REQUIRED;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ResetPasswordRequest {

  @NotBlank(message = REQUIRED)
  private String resetToken;

  @NotBlank(message = REQUIRED)
  @Size(min = 8, max = 72, message = "Mật khẩu phải có từ 8 đến 72 ký tự")
  private String newPassword;

  @NotBlank(message = REQUIRED)
  private String confirmPassword;
}
