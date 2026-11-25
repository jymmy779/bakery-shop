package com.bakery.notification;

import jakarta.mail.MessagingException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/** Scoped order-mail service; does not implement the unrelated registration/OTP use cases. */
@Service
public class OrderConfirmationMailSender {
  private final ObjectProvider<JavaMailSender> senders;
  private final String from;
  private final String host;

  public OrderConfirmationMailSender(ObjectProvider<JavaMailSender> senders,
      @Value("${app.mail.from:}") String from, @Value("${spring.mail.host:}") String host) {
    this.senders = senders;
    this.from = from;
    this.host = host;
  }

  public void send(OrderConfirmationMail mail) {
    var sender = senders.getIfAvailable();
    if (sender == null || from.isBlank() || host.isBlank()) {
      throw new IllegalStateException("SMTP host/sender has not been configured");
    }
    String body = mail.body() == null ? "" : mail.body();
    if (body.stripLeading().startsWith("<!DOCTYPE html") || body.stripLeading().startsWith("<html")) {
      try {
        var mime = sender.createMimeMessage();
        var helper = new MimeMessageHelper(mime, "UTF-8");
        helper.setFrom(from);
        helper.setTo(mail.recipient());
        helper.setSubject(mail.subject());
        helper.setText(body, true);
        sender.send(mime);
        return;
      } catch (MessagingException ex) {
        throw new MailSendException("Failed to compose HTML order confirmation email", ex);
      }
    }
    var message = new SimpleMailMessage();
    message.setFrom(from);
    message.setTo(mail.recipient());
    message.setSubject(mail.subject());
    message.setText(body);
    sender.send(message);
  }
}
