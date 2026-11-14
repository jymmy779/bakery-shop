package com.bakery.order.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.bakery.order.dto.OrderDetailResponse;
import com.bakery.order.dto.PaymentResponse;
import com.bakery.order.entity.OrderStatus;
import com.bakery.order.entity.PaymentMethod;
import com.bakery.order.entity.PaymentStatus;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class BankTransferServiceImplTest {

  private final BankTransferServiceImpl transfers = new BankTransferServiceImpl(bank("970436", "0000000000000", "qr_only"));

  @Test
  void buildsSvgFromExactStoredAmountAndOrderCodePreservingAccountLeadingZeroes() {
    var order = order();
    var result = transfers.prepare(order);
    assertTrue(result.isAvailable());
    assertEquals("0000000000000", result.getAccountNumber());
    assertEquals("500000", result.getAmount());
    assertEquals(order.getOrderCode(), result.getContent());
    var uri = URI.create(result.getQrImageUrl());
    assertEquals("https", uri.getScheme());
    assertEquals("img.vietqr.io", uri.getHost());
    assertEquals("/image/970436-0000000000000-qr_only.svg", uri.getPath());
    var query = Arrays.stream(uri.getRawQuery().split("&")).map(item -> item.split("=", 2))
        .collect(Collectors.toMap(item -> item[0], item -> URLDecoder.decode(item[1], StandardCharsets.UTF_8)));
    assertEquals("500000", query.get("amount"));
    assertEquals("LNABC123456", query.get("addInfo"));
    assertEquals("LUNE & BÁNH TEST", query.get("accountName"));
    assertFalse(result.getQrImageUrl().contains(order.getReceiverPhone()));
    assertFalse(result.getQrImageUrl().contains(order.getReceiverEmail()));
    assertEquals(PaymentStatus.PENDING, order.getPayment().getStatus());
    assertEquals(OrderStatus.PENDING, order.getStatus());
  }

  @ParameterizedTest
  @EnumSource(value = OrderStatus.class, names = "PENDING", mode = EnumSource.Mode.EXCLUDE)
  void neverOffersTransferForOrdersThatAreNoLongerPending(OrderStatus status) {
    var order = order();
    order.setStatus(status);
    assertUnavailable(transfers.prepare(order));
  }

  @ParameterizedTest
  @EnumSource(OrderStatus.class)
  void neverOffersASecondTransferForAPaidOrder(OrderStatus status) {
    var order = order();
    order.setStatus(status);
    order.getPayment().setStatus(PaymentStatus.PAID);
    assertUnavailable(transfers.prepare(order));
  }

  @Test
  void missingConfigurationKeepsThePageUsableWithoutInventingABankAccount() {
    var service = new BankTransferServiceImpl(new BankTransferProperties("", "", "", "", "qr_only"));
    var result = service.prepare(order());
    assertUnavailable(result);
    assertTrue(result.getNotice().contains("đang được cập nhật"));
  }

  @ParameterizedTest
  @MethodSource("unsafeBankSettings")
  void malformedPathSettingsCannotInjectAnExternalUrl(BankTransferProperties settings) {
    assertUnavailable(new BankTransferServiceImpl(settings).prepare(order()));
  }

  static Stream<BankTransferProperties> unsafeBankSettings() {
    return Stream.of(bank("https://example.test", "00000", "qr_only"),
        bank("970436", "000/../123", "qr_only"), bank("970436", "00000", "../../x"),
        bank("970436", "00000000000000000000", "qr_only"));
  }

  @ParameterizedTest
  @MethodSource("invalidAmounts")
  void neverRoundsOrGeneratesQrForInvalidAmounts(BigDecimal amount) {
    var order = order();
    order.setTotalAmount(amount);
    order.getPayment().setAmount(amount);
    assertUnavailable(transfers.prepare(order));
  }

  static Stream<BigDecimal> invalidAmounts() {
    return Stream.of(null, BigDecimal.ZERO, new BigDecimal("-1"),
        new BigDecimal("500000.50"), new BigDecimal("10000000000000"));
  }

  @Test
  void mismatchedPaymentAmountCannotBecomeAPayableQr() {
    var order = order();
    order.getPayment().setAmount(BigDecimal.ONE);
    assertUnavailable(transfers.prepare(order));
  }

  @Test
  void missingPaymentAndUnsafeTransferContentDoNotGenerateQr() {
    var order = order();
    order.setOrderCode("LN123&amount=1");
    assertUnavailable(transfers.prepare(order));
    order.setPayment(null);
    assertUnavailable(transfers.prepare(order));
  }

  private void assertUnavailable(com.bakery.order.dto.BankTransferResponse response) {
    assertFalse(response.isAvailable());
    assertNotNull(response.getNotice());
    assertNull(response.getAccountNumber());
    assertNull(response.getQrImageUrl());
  }

  private static BankTransferProperties bank(String id, String account, String template) {
    return new BankTransferProperties(id, "Ngân hàng kiểm thử", account, "LUNE & BÁNH TEST", template);
  }

  private OrderDetailResponse order() {
    var order = new OrderDetailResponse();
    order.setStatus(OrderStatus.PENDING);
    order.setOrderCode("LNABC123456");
    order.setReceiverPhone("0912345678");
    order.setReceiverEmail("private@example.com");
    order.setTotalAmount(new BigDecimal("500000.00"));
    var payment = new PaymentResponse();
    payment.setMethod(PaymentMethod.BANK_TRANSFER);
    payment.setStatus(PaymentStatus.PENDING);
    payment.setAmount(new BigDecimal("500000"));
    order.setPayment(payment);
    return order;
  }
}
