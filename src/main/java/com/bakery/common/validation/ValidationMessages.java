package com.bakery.common.validation;

public final class ValidationMessages {

  public static final String REQUIRED = "Vui lòng nhập thông tin bắt buộc";
  public static final String INVALID_EMAIL = "Email không đúng định dạng";
  public static final String INVALID_PHONE = "Số điện thoại không đúng định dạng";
  public static final String POSITIVE_PRICE = "Giá bán phải lớn hơn 0";
  public static final String NON_NEGATIVE_STOCK = "Số lượng tồn kho không được âm";

  private ValidationMessages() {
  }
}
