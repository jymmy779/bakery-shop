package com.bakery.notification.impl;

import com.bakery.account.entity.Account;
import com.bakery.notification.EmailService;
import com.bakery.order.entity.Order;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SmtpEmailService implements EmailService {

  private final JavaMailSender mailSender;

  @Value("${app.mail.from:${spring.mail.username:}}")
  private String fromAddress;

  @Override
  public void sendRegistrationConfirmation(Account account) {
    sendText(account.getEmail(), "Xác nhận đăng ký tài khoản Bakery Shop",
        "Xin chào " + account.getFullName() + ",\n\nTài khoản Bakery Shop của bạn đã được tạo.");
  }

  @Override
  public void sendPasswordResetOtp(Account account, String otp) {
    sendHtml(account.getEmail(), "Mã OTP đặt lại mật khẩu Bakery Shop",
        "<p>Xin chào " + escapeHtml(account.getFullName()) + ",</p>"
            + "<p>Mã xác minh đặt lại mật khẩu của bạn là:</p>"
            + "<p style=\"font-size:28px;font-weight:bold;letter-spacing:8px\">" + otp + "</p>"
            + "<p>Mã có hiệu lực trong 10 phút. Nếu bạn không yêu cầu, hãy bỏ qua email này.</p>");
  }

  @Override
  public void sendOrderConfirmation(Order order) {
    if (order.getReceiverEmail() == null || order.getReceiverEmail().isBlank()) {
      return;
    }
    sendText(order.getReceiverEmail(), "Xác nhận đơn hàng " + order.getOrderCode(),
        "Đơn hàng " + order.getOrderCode() + " đã được ghi nhận. Tổng thanh toán: "
            + order.getTotalAmount() + " VND.");
  }

  private void sendText(String recipient, String subject, String content) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(requireFromAddress());
    message.setTo(recipient);
    message.setSubject(subject);
    message.setText(content);
    try {
      mailSender.send(message);
    } catch (MailException exception) {
      throw new IllegalStateException("Không gửi được email. Hãy kiểm tra cấu hình SMTP.", exception);
    }
  }

  private void sendHtml(String recipient, String subject, String html) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
      helper.setFrom(requireFromAddress());
      helper.setTo(recipient);
      helper.setSubject(subject);
      helper.setText(html, true);
      mailSender.send(message);
    } catch (Exception exception) {
      throw new IllegalStateException("Không gửi được email OTP. Hãy kiểm tra cấu hình SMTP.", exception);
    }
  }

  private String requireFromAddress() {
    if (fromAddress == null || fromAddress.isBlank()) {
      throw new IllegalStateException("Thiếu MAIL_FROM hoặc MAIL_USERNAME trong cấu hình chạy ứng dụng");
    }
    return fromAddress;
  }

  private String escapeHtml(String value) {
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&#39;");
  }
}
