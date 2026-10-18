package com.bakery.account.dto;

import com.bakery.account.entity.AccountStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AccountStatusUpdateRequest {

  @NotNull(message = "Vui lòng chọn trạng thái tài khoản")
  private AccountStatus status;
}
