package com.bakery.notification;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
    var message = new SimpleMailMessage();
    message.setFrom(from);
    message.setTo(mail.recipient());
    message.setSubject(mail.subject());
    message.setText(mail.body());
    sender.send(message);
  }
}
