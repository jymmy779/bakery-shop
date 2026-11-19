package com.bakery.order.service.impl;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Optional settings so local startup and order creation still work before the shop adds its bank. */
@Component
@Getter
public class BankTransferProperties {

  private final String bankId;
  private final String bankName;
  private final String accountNumber;
  private final String accountName;
  private final String qrTemplate;

  public BankTransferProperties(
      @Value("${app.payment.bank-id:${PAYMENT_BANK_ID:}}") String bankId,
      @Value("${app.payment.bank-name:${PAYMENT_BANK_NAME:}}") String bankName,
      @Value("${app.payment.account-number:${PAYMENT_ACCOUNT_NUMBER:}}") String accountNumber,
      @Value("${app.payment.account-name:${PAYMENT_ACCOUNT_NAME:}}") String accountName,
      @Value("${app.payment.qr-template:${PAYMENT_QR_TEMPLATE:qr_only}}") String qrTemplate) {
    this.bankId = clean(bankId);
    this.bankName = clean(bankName);
    this.accountNumber = clean(accountNumber);
    this.accountName = clean(accountName);
    this.qrTemplate = clean(qrTemplate);
  }

  private static String clean(String value) {
    return value == null ? "" : value.trim();
  }
}
