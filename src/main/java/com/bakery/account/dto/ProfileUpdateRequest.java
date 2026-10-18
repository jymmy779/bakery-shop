package com.bakery.account.dto;

import static com.bakery.common.validation.ValidationMessages.INVALID_PHONE;
import static com.bakery.common.validation.ValidationMessages.REQUIRED;

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
public class ProfileUpdateRequest {

  @NotBlank(message = REQUIRED)
  @Size(max = 100)
  private String fullName;

  @NotBlank(message = REQUIRED)
  @Pattern(regexp = ValidationPatterns.PHONE, message = INVALID_PHONE)
  private String phone;

  @NotBlank(message = REQUIRED)
  @Size(max = 500)
  private String address;
}
