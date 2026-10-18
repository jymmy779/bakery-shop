package com.bakery.catalog.dto;

import static com.bakery.common.validation.ValidationMessages.REQUIRED;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CategoryRequest {

  @NotBlank(message = REQUIRED)
  @Size(max = 100)
  private String name;

  private String description;
}
