package com.bakery.account.dto;

import static com.bakery.common.validation.ValidationMessages.INVALID_EMAIL;
import static com.bakery.common.validation.ValidationMessages.INVALID_PHONE;
import static com.bakery.common.validation.ValidationMessages.REQUIRED;

import com.bakery.common.validation.ValidationPatterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegisterRequest {

  @NotBlank(message = REQUIRED)
  @Size(max = 100)
  private String fullName;

  @NotBlank(message = REQUIRED)
  @Email(message = INVALID_EMAIL)
  @Size(max = 150)
  private String email;

  @NotBlank(message = REQUIRED)
  @Pattern(regexp = ValidationPatterns.PHONE, message = INVALID_PHONE)
  private String phone;

  @Size(max = 500)
  private String address;

  @NotBlank(message = REQUIRED)
  @Size(min = 8, max = 72, message = "Mật khẩu phải có từ 8 đến 72 ký tự")
  private String password;

  @NotBlank(message = REQUIRED)
  private String confirmPassword;
}
