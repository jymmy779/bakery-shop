package com.bakery.order.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Customer instructions only; viewing or copying these values never confirms a payment. */
@Getter
@Setter
@NoArgsConstructor
public class BankTransferResponse {

  private boolean available;
  private String notice;
  private String bankName;
  private String accountNumber;
  private String accountName;
  private String amount;
  private String content;
  private String qrImageUrl;
}
