package com.bakery.account.dto;

import static com.bakery.common.validation.ValidationMessages.REQUIRED;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChangePasswordRequest {

  @NotBlank(message = REQUIRED)
  private String currentPassword;

  @NotBlank(message = REQUIRED)
  @Size(min = 8, max = 72, message = "Mật khẩu mới phải có từ 8 đến 72 ký tự")
  private String newPassword;

  @NotBlank(message = REQUIRED)
  @Size(min = 8, max = 72, message = "Mật khẩu xác nhận phải có từ 8 đến 72 ký tự")
  private String confirmPassword;

  @AssertTrue(message = "Mật khẩu xác nhận không khớp")
  public boolean isNewPasswordConfirmed() {
    return newPassword == null || confirmPassword == null || newPassword.equals(confirmPassword);
  }
}
