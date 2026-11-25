package com.bakery.account.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AccountAddressResponse {

  private Long id;
  private String label;
  private String receiverName;
  private String receiverPhone;
  private String address;
  private String deliveryNote;
  private boolean defaultAddress;
}
