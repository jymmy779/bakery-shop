package com.bakery.account.dto;

import com.bakery.account.entity.AccountStatus;
import com.bakery.account.entity.Role;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AccountProfileResponse {

  private Long id;
  private String email;
  private String fullName;
  private String phone;
  private String address;
  private Role role;
  private AccountStatus status;
}
