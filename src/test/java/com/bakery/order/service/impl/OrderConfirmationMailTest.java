package com.bakery.order.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.bakery.notification.OrderConfirmationMail;
import com.bakery.notification.OrderConfirmationMailSender;
import com.bakery.order.dto.OrderDetailResponse;
import com.bakery.order.dto.OrderItemResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class OrderConfirmationMailTest {
  private final OrderConfirmationMailSender sender = mock(OrderConfirmationMailSender.class);
  private final BankTransferProperties bank = new BankTransferProperties("970436", "Ngân hàng kiểm thử", "0000000000", "LUNE TEST", "qr_only");
  private final Executor direct = Runnable::run;

  @BeforeEach void begin() {
    TransactionSynchronizationManager.initSynchronization();
    TransactionSynchronizationManager.setActualTransactionActive(true);
  }
  @AfterEach void end() {
    TransactionSynchronizationManager.clearSynchronization();
    TransactionSynchronizationManager.setActualTransactionActive(false);
  }
  private void commit() {
    TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
  }

  @Test void immutableSnapshotContainsSavedDetailsDeliveryAndBankInstructions() {
    var order = order();
    new OrderConfirmationNotifier(sender, direct, bank, true).afterCheckout(order);
    verifyNoInteractions(sender);
    order.setReceiverName("Changed after registration"); order.getItems().get(0).setProductName("Renamed later");
    commit();
    var captured = ArgumentCaptor.forClass(OrderConfirmationMail.class); verify(sender).send(captured.capture());
    var mail = captured.getValue();
    assertEquals("customer@example.test", mail.recipient());
    assertEquals("LUNE Bakery — Xác nhận đơn LNTEST001", mail.subject());
    for (String text : List.of("Bánh kem dâu", "SL: 2", "450.000 VND", "900.000 VND", "Khách nhận bánh",
        "0912345678", "12 Nguyễn Huệ", "18/11/2026", "15:30", "Ít ngọt", "Gọi trước khi giao", "0000000000", "LUNE TEST", "Nội dung: LNTEST001")) {
      assertTrue(mail.body().contains(text), text);
    }
    assertFalse(mail.body().contains("Renamed later"));
    assertFalse(mail.body().contains("Changed after registration"));
    assertTrue(mail.body().contains("không phải xác nhận đã thanh toán"));
  }

  @Test void rollbackDoesNotSendEvenWhenEmailWasRegistered() {
    new OrderConfirmationNotifier(sender, direct, bank, true).afterCheckout(order());
    TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
    verifyNoInteractions(sender);
  }
  @Test void disabledOrMissingRecipientDoesNotRegisterWork() {
    new OrderConfirmationNotifier(sender, direct, bank, false).afterCheckout(order());
    var empty = order(); empty.setReceiverEmail(null);
    new OrderConfirmationNotifier(sender, direct, bank, true).afterCheckout(empty);
    empty.setReceiverEmail("   "); new OrderConfirmationNotifier(sender, direct, bank, true).afterCheckout(empty);
    assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty()); verifyNoInteractions(sender);
  }
  @Test void smtpFailureCannotEscapeAfterCommit() {
    doThrow(new org.springframework.mail.MailSendException("failed")).when(sender).send(any());
    new OrderConfirmationNotifier(sender, direct, bank, true).afterCheckout(order());
    assertDoesNotThrow(this::commit); verify(sender).send(any());
  }
  @Test void saturatedExecutorCannotTurnCommittedCheckoutIntoError() {
    Executor rejected = task -> { throw new RejectedExecutionException("full"); };
    new OrderConfirmationNotifier(sender, rejected, bank, true).afterCheckout(order());
    assertDoesNotThrow(this::commit); verifyNoInteractions(sender);
  }
  @Test void missingBankAndOptionalFieldsStillSendValidConfirmation() {
    var order = order(); order.setDeliveryTime(null); order.setNote(null); order.getItems().get(0).setNote(null);
    var blankBank = new BankTransferProperties("", "", "", "", "qr_only");
    new OrderConfirmationNotifier(sender, direct, blankBank, true).afterCheckout(order);
    commit();
    var captured = ArgumentCaptor.forClass(OrderConfirmationMail.class); verify(sender).send(captured.capture());
    assertTrue(captured.getValue().body().contains("Thông tin chuyển khoản đang được cập nhật"));
    assertFalse(captured.getValue().body().contains("Số tài khoản:"));
  }
  @Test void absentTransactionDoesNotSendPrematureMail() {
    TransactionSynchronizationManager.setActualTransactionActive(false);
    new OrderConfirmationNotifier(sender, direct, bank, true).afterCheckout(order());
    assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty()); verifyNoInteractions(sender);
  }
  private OrderDetailResponse order() {
    var order = new OrderDetailResponse(); order.setOrderCode("LNTEST001"); order.setReceiverEmail("customer@example.test");
    order.setReceiverName("Khách nhận bánh"); order.setReceiverPhone("0912345678"); order.setReceiverAddress("12 Nguyễn Huệ");
    order.setDeliveryDate(LocalDate.of(2026, 11, 18)); order.setDeliveryTime(LocalTime.of(15, 30));
    order.setTotalAmount(new BigDecimal("900000")); order.setNote("Gọi trước khi giao");
    var item = new OrderItemResponse(); item.setProductName("Bánh kem dâu"); item.setQuantity(2);
    item.setUnitPrice(new BigDecimal("450000")); item.setLineTotal(order.getTotalAmount()); item.setNote("Ít ngọt");
    order.setItems(List.of(item)); return order;
  }
}
