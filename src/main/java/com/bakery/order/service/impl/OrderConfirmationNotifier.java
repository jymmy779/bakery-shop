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
import org.springframework.web.util.HtmlUtils;

@Service
@Slf4j
public class OrderConfirmationNotifier {
  private final OrderConfirmationMailSender sender;
  private final Executor executor;
  private final boolean enabled;

  public OrderConfirmationNotifier(OrderConfirmationMailSender sender,
      @Qualifier("orderConfirmationExecutor") Executor executor,
      @Value("${app.mail.order-confirmation-enabled:false}") boolean enabled) {
    this.sender = sender;
    this.executor = executor;
    this.enabled = enabled;
  }

  public void afterPaymentConfirmed(OrderDetailResponse order) {
    if (!enabled || order.getReceiverEmail() == null || order.getReceiverEmail().isBlank()) {
      return;
    }
    String code = order.getOrderCode();
    try {
      // Build a complete immutable HTML snapshot while the payment transaction still has all saved order data.
      var mail = compose(order);
      if (!TransactionSynchronizationManager.isActualTransactionActive()
          || !TransactionSynchronizationManager.isSynchronizationActive()) {
        throw new IllegalStateException("Order mail must be registered inside payment transaction");
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
    log.warn("Order {} remains saved; payment confirmation email was not sent ({}). Check SMTP configuration/availability.",
        code, failure.getClass().getSimpleName());
  }

  private OrderConfirmationMail compose(OrderDetailResponse order) {
    var money = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"));
    String orderCode = esc(order.getOrderCode());
    String receiverName = esc(order.getReceiverName());
    String receiverPhone = esc(order.getReceiverPhone());
    String receiverAddress = esc(order.getReceiverAddress());
    String deliveryDate = order.getDeliveryDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    String deliveryTime = order.getDeliveryTime() != null
        ? " lúc " + order.getDeliveryTime().format(DateTimeFormatter.ofPattern("HH:mm"))
        : "";

    var itemsHtml = new StringBuilder();
    for (var item : order.getItems()) {
      itemsHtml.append("""
          <tr>
            <td style="padding:12px 0;border-bottom:1px solid #f2e4df;vertical-align:top;">
              <div style="font-weight:600;color:#261814;font-size:14px;">%s</div>
              <div style="color:#564244;font-size:12px;margin-top:2px;">SL: %d &times; %s VND</div>
              %s
            </td>
            <td style="padding:12px 0;border-bottom:1px solid #f2e4df;text-align:right;font-weight:600;color:#6c0c28;font-size:14px;white-space:nowrap;vertical-align:top;">
              %s VND
            </td>
          </tr>
          """.formatted(
          esc(item.getProductName()),
          item.getQuantity(),
          money.format(item.getUnitPrice()),
          item.getNote() != null && !item.getNote().isBlank()
              ? "<div style=\"color:#842039;font-size:12px;margin-top:4px;\">Lời nhắn: " + esc(item.getNote()) + "</div>"
              : "",
          money.format(item.getLineTotal())));
    }

    String bankRefRow = "";
    if (order.getPayment() != null && order.getPayment().getBankReference() != null
        && !order.getPayment().getBankReference().isBlank()) {
      bankRefRow = """
          <tr>
            <td style="padding:4px 0;color:#564244;font-size:13px;">Mã giao dịch</td>
            <td style="padding:4px 0;color:#261814;font-size:13px;font-weight:600;text-align:right;">%s</td>
          </tr>
          """.formatted(esc(order.getPayment().getBankReference()));
    }

    String paidAtRow = "";
    if (order.getPayment() != null && order.getPayment().getPaidAt() != null) {
      paidAtRow = """
          <tr>
            <td style="padding:4px 0;color:#564244;font-size:13px;">Thời gian thanh toán</td>
            <td style="padding:4px 0;color:#261814;font-size:13px;font-weight:600;text-align:right;">%s</td>
          </tr>
          """.formatted(order.getPayment().getPaidAt().format(DateTimeFormatter.ofPattern("HH:mm — dd/MM/yyyy")));
    }

    String orderNoteRow = order.getNote() != null && !order.getNote().isBlank()
        ? """
            <tr>
              <td style="padding:4px 0;color:#564244;font-size:13px;">Ghi chú đơn</td>
              <td style="padding:4px 0;color:#261814;font-size:13px;text-align:right;">%s</td>
            </tr>
            """.formatted(esc(order.getNote()))
        : "";

    String html = """
        <!DOCTYPE html>
        <html lang="vi">
        <head><meta charset="UTF-8"/></head>
        <body style="margin:0;padding:0;background-color:#fff8f6;font-family:'Plus Jakarta Sans',Arial,sans-serif;color:#261814;">
          <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color:#fff8f6;padding:28px 12px;">
            <tr>
              <td align="center">
                <table role="presentation" width="600" cellspacing="0" cellpadding="0" style="max-width:600px;width:100%%;background-color:#ffffff;border-radius:16px;overflow:hidden;border:1px solid #f2e4df;">
                  <tr>
                    <td style="background-color:#6F1930;padding:28px 32px;text-align:center;">
                      <div style="font-family:Georgia,serif;font-size:26px;font-weight:700;letter-spacing:2px;color:#fff8f6;">LUNE BAKERY</div>
                      <div style="font-size:11px;letter-spacing:2px;text-transform:uppercase;color:#ffb2bc;margin-top:6px;">Xác nhận thanh toán &amp; đơn hàng</div>
                    </td>
                  </tr>
                  <tr>
                    <td style="padding:28px 32px;">
                      <div style="display:inline-block;background-color:#d5e9bf;color:#3b4c2d;font-size:12px;font-weight:700;padding:6px 14px;border-radius:999px;margin-bottom:14px;">
                        &#10003; Đã thanh toán &amp; Đã xác nhận
                      </div>
                      <h1 style="margin:0 0 8px;font-family:Georgia,serif;font-size:22px;color:#261814;">Cảm ơn %s đã đặt bánh tại LUNE!</h1>
                      <p style="margin:0 0 20px;font-size:14px;line-height:1.6;color:#564244;">
                        Chúng tôi đã nhận được thanh toán chuyển khoản của bạn. Bếp bánh LUNE đang chuẩn bị những chiếc bánh tươi ngon nhất cho đơn <strong>%s</strong>.
                      </p>

                      <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color:#fff1ec;border-radius:12px;padding:16px;margin-bottom:22px;">
                        <tr>
                          <td style="padding:4px 0;color:#564244;font-size:13px;">Mã đơn hàng</td>
                          <td style="padding:4px 0;color:#6c0c28;font-size:14px;font-weight:700;text-align:right;">%s</td>
                        </tr>
                        %s
                        %s
                        <tr>
                          <td style="padding:4px 0;color:#564244;font-size:13px;">Trạng thái</td>
                          <td style="padding:4px 0;color:#3b4c2d;font-size:13px;font-weight:700;text-align:right;">Đã xác nhận · Đã thanh toán</td>
                        </tr>
                      </table>

                      <div style="font-size:12px;font-weight:700;letter-spacing:1px;text-transform:uppercase;color:#564244;margin-bottom:8px;">Bánh đã chọn</div>
                      <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="margin-bottom:18px;">
                        %s
                        <tr>
                          <td style="padding:14px 0 0;font-size:15px;font-weight:700;color:#261814;">Tổng đã thanh toán</td>
                          <td style="padding:14px 0 0;font-size:18px;font-weight:700;color:#6c0c28;text-align:right;">%s VND</td>
                        </tr>
                      </table>

                      <div style="font-size:12px;font-weight:700;letter-spacing:1px;text-transform:uppercase;color:#564244;margin:22px 0 8px;">Thông tin nhận bánh</div>
                      <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color:#fff8f6;border:1px solid #f2e4df;border-radius:12px;padding:14px 16px;">
                        <tr>
                          <td style="padding:4px 0;color:#564244;font-size:13px;">Người nhận</td>
                          <td style="padding:4px 0;color:#261814;font-size:13px;font-weight:600;text-align:right;">%s</td>
                        </tr>
                        <tr>
                          <td style="padding:4px 0;color:#564244;font-size:13px;">Điện thoại</td>
                          <td style="padding:4px 0;color:#261814;font-size:13px;font-weight:600;text-align:right;">%s</td>
                        </tr>
                        <tr>
                          <td style="padding:4px 0;color:#564244;font-size:13px;">Địa chỉ</td>
                          <td style="padding:4px 0;color:#261814;font-size:13px;text-align:right;">%s</td>
                        </tr>
                        <tr>
                          <td style="padding:4px 0;color:#564244;font-size:13px;">Ngày nhận</td>
                          <td style="padding:4px 0;color:#261814;font-size:13px;font-weight:600;text-align:right;">%s%s</td>
                        </tr>
                        %s
                      </table>

                      <p style="margin:22px 0 0;font-size:13px;line-height:1.6;color:#564244;">
                        Bạn có thể đăng nhập website <strong>LUNE Bakery &rarr; Đơn hàng của tôi</strong> để theo dõi tiến trình làm bánh và giao hàng mới nhất.
                      </p>
                    </td>
                  </tr>
                  <tr>
                    <td style="background-color:#fff1ec;padding:16px 32px;text-align:center;font-size:12px;color:#564244;">
                      LUNE Bakery &amp; Pâtisserie &bull; Nướng mới mỗi ngày, ngọt lành trong từng khoảnh khắc
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
          </table>
        </body>
        </html>
        """.formatted(
        receiverName,
        orderCode,
        orderCode,
        bankRefRow,
        paidAtRow,
        itemsHtml.toString(),
        money.format(order.getTotalAmount()),
        receiverName,
        receiverPhone,
        receiverAddress,
        deliveryDate,
        deliveryTime,
        orderNoteRow);

    return new OrderConfirmationMail(
        order.getReceiverEmail().trim(),
        "LUNE Bakery — Thanh toán thành công đơn " + order.getOrderCode(),
        html);
  }

  private String esc(String value) {
    return value == null ? "" : HtmlUtils.htmlEscape(value, "UTF-8");
  }
}
