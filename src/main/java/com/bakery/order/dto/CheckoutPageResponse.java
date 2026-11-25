package com.bakery.order.dto;

import com.bakery.account.dto.AccountAddressResponse;
import com.bakery.cart.dto.CartResponse;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutPageResponse {

  private CartResponse cart;
  private CheckoutRequest request;
  private List<AccountAddressResponse> savedAddresses;
  private Long selectedAddressId;
  private LocalDate earliestDeliveryDate;
  private boolean addressUnavailable;
}
