package com.bakery.account.dto;

import static com.bakery.common.validation.ValidationMessages.INVALID_EMAIL;
import static com.bakery.common.validation.ValidationMessages.REQUIRED;

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
public class VerifyOtpRequest {

  @NotBlank(message = REQUIRED)
  @Email(message = INVALID_EMAIL)
  @Size(max = 150)
  private String email;

  @NotBlank(message = REQUIRED)
  @Pattern(regexp = "^[0-9]{6}$", message = "OTP phải gồm 6 chữ số")
  private String otp;
}
