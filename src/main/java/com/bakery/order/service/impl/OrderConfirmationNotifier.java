package com.bakery.order.service.impl;

import com.bakery.notification.OrderConfirmationMail;
import com.bakery.notification.OrderConfirmationMailSender;
import com.bakery.order.dto.OrderDetailResponse;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@Slf4j
public class OrderConfirmationNotifier {
  private final OrderConfirmationMailSender sender;
  private final Executor executor;
  private final BankTransferProperties bank;
  private final boolean enabled;

  public OrderConfirmationNotifier(OrderConfirmationMailSender sender,
      @Qualifier("orderConfirmationExecutor") Executor executor, BankTransferProperties bank,
      @Value("${app.mail.order-confirmation-enabled:false}") boolean enabled) {
    this.sender = sender;
    this.executor = executor;
    this.bank = bank;
    this.enabled = enabled;
  }

  public void afterCheckout(OrderDetailResponse order) {
    if (!enabled || order.getReceiverEmail() == null || order.getReceiverEmail().isBlank())
      return;
    String code = order.getOrderCode();
    try {
      // Build a complete immutable snapshot while checkout still has all saved order
      // data.
      var mail = compose(order);
      if (!TransactionSynchronizationManager.isActualTransactionActive()
          || !TransactionSynchronizationManager.isSynchronizationActive()) {
        throw new IllegalStateException("Order mail must be registered inside checkout transaction");
      }
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          try {
            executor.execute(() -> {
              try {
                sender.send(mail);
              } catch (RuntimeException failure) {
                report(code, failure);
              }
            });
          } catch (RuntimeException failure) {
            report(code, failure);
          }
        }
      });
    } catch (RuntimeException failure) {
      report(code, failure);
    }
  }

  private void report(String code, RuntimeException failure) {
    // Avoid logging recipient details, SMTP credentials or raw provider responses.
    log.warn("Order {} remains saved; confirmation email was not sent ({}). Check SMTP configuration/availability.",
        code, failure.getClass().getSimpleName());
  }

  private OrderConfirmationMail compose(OrderDetailResponse order) {
    var money = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"));
    var body = new StringBuilder("LUNE BAKERY — XÁC NHẬN ĐẶT HÀNG\n\n");
    body.append("Cảm ơn bạn đã đặt bánh tại LUNE!\nMã đơn hàng: ").append(order.getOrderCode());
    body.append("\nTrạng thái lúc đặt: Chờ xử lý / Chờ thanh toán\n\nBÁNH ĐÃ CHỌN\n");
    for (var item : order.getItems()) {
      body.append("- ").append(item.getProductName()).append(" | SL: ").append(item.getQuantity())
          .append(" | Đơn giá: ").append(money.format(item.getUnitPrice())).append(" VND")
          .append(" | Thành tiền: ").append(money.format(item.getLineTotal())).append(" VND\n");
      if (item.getNote() != null)
        body.append("  Lời nhắn: ").append(item.getNote()).append('\n');
    }
    body.append("\nTổng tiền: ").append(money.format(order.getTotalAmount())).append(" VND\n\nTHÔNG TIN NHẬN BÁNH\n")
        .append("Người nhận: ").append(order.getReceiverName()).append("\nĐiện thoại: ")
        .append(order.getReceiverPhone())
        .append("\nĐịa chỉ: ").append(order.getReceiverAddress()).append("\nNgày nhận: ")
        .append(order.getDeliveryDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    if (order.getDeliveryTime() != null)
      body.append(" lúc ").append(order.getDeliveryTime().format(DateTimeFormatter.ofPattern("HH:mm")));
    if (order.getNote() != null)
      body.append("\nGhi chú đơn: ").append(order.getNote());
    body.append("\n\nTHANH TOÁN CHUYỂN KHOẢN\n");
    if (!bank.getBankName().isBlank() && !bank.getAccountNumber().isBlank() && !bank.getAccountName().isBlank()) {
      body.append("Ngân hàng: ").append(bank.getBankName()).append("\nSố tài khoản: ").append(bank.getAccountNumber())
          .append("\nChủ tài khoản: ").append(bank.getAccountName()).append("\nSố tiền: ")
          .append(money.format(order.getTotalAmount())).append(" VND\nNội dung: ").append(order.getOrderCode())
          .append("\nVui lòng chuyển đúng số tiền và nội dung. Cửa hàng sẽ đối soát và xác nhận đã nhận tiền.\n");
    } else
      body.append("Thông tin chuyển khoản đang được cập nhật. Vui lòng xem lại trên website trước khi chuyển tiền.\n");
    body.append("\nĐây là email xác nhận đã đặt đơn, không phải xác nhận đã thanh toán.")
        .append("\nĐăng nhập website LUNE → Đơn hàng của tôi để xem QR và trạng thái mới nhất.");
    return new OrderConfirmationMail(order.getReceiverEmail().trim(),
        "LUNE Bakery — Xác nhận đơn " + order.getOrderCode(), body.toString());
  }
}
