package com.bakery.order.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.bakery.notification.OrderConfirmationMail;
import com.bakery.notification.OrderConfirmationMailSender;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

class OrderMailTransportTest {
  @SuppressWarnings("unchecked")
  private ObjectProvider<JavaMailSender> provider(JavaMailSender sender) {
    ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
    when(provider.getIfAvailable()).thenReturn(sender); return provider;
  }

  @Test void missingSmtpOrSenderIsRejectedWithoutNetworkRequest() {
    var sender = mock(JavaMailSender.class);
    var mail = new OrderConfirmationMail("customer@example.test", "Order", "Body");
    assertThrows(IllegalStateException.class,
        () -> new OrderConfirmationMailSender(provider(sender), "", "localhost").send(mail));
    assertThrows(IllegalStateException.class,
        () -> new OrderConfirmationMailSender(provider(sender), "shop@example.test", "").send(mail));
    assertThrows(IllegalStateException.class,
        () -> new OrderConfirmationMailSender(provider(null), "shop@example.test", "localhost").send(mail));
    verifyNoInteractions(sender);
  }

  @Test void deliversVietnameseSubjectAndBodyToIsolatedLocalSmtpServer() throws Exception {
    var pool = Executors.newSingleThreadExecutor();
    try (var server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
      server.setSoTimeout(10000);
      var received = pool.submit(() -> {
        try (var socket = server.accept()) {
          socket.setSoTimeout(10000);
          var input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
          var output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII), true);
          output.print("220 localhost test SMTP\r\n"); output.flush();
          var data = new StringBuilder(); boolean body = false;
          for (String line; (line = input.readLine()) != null;) {
            if (body) {
              if (line.equals(".")) { body = false; output.print("250 accepted\r\n"); output.flush(); }
              else data.append(line.startsWith("..") ? line.substring(1) : line).append("\r\n");
            } else if (line.equals("DATA")) { body = true; output.print("354 send body\r\n"); output.flush(); }
            else if (line.equals("QUIT")) { output.print("221 bye\r\n"); output.flush(); break; }
            else { output.print("250 OK\r\n"); output.flush(); }
          }
          return data.toString();
        }
      });
      var smtp = new JavaMailSenderImpl(); smtp.setHost("127.0.0.1"); smtp.setPort(server.getLocalPort());
      smtp.setDefaultEncoding("UTF-8");
      smtp.getJavaMailProperties().setProperty("mail.smtp.connectiontimeout", "5000");
      smtp.getJavaMailProperties().setProperty("mail.smtp.timeout", "5000");
      String subject = "LUNE — Xác nhận đơn LNTEST001";
      String body = "Cảm ơn bạn!\nBánh kem dâu × 2\nTổng tiền: 900.000 VND";
      new OrderConfirmationMailSender(provider(smtp), "shop@example.test", "127.0.0.1")
          .send(new OrderConfirmationMail("customer@example.test", subject, body));
      var message = new MimeMessage(Session.getInstance(new Properties()),
          new ByteArrayInputStream(received.get(10, TimeUnit.SECONDS).getBytes(StandardCharsets.US_ASCII)));
      assertEquals(subject, message.getSubject());
      assertEquals("customer@example.test", message.getAllRecipients()[0].toString());
      assertEquals("shop@example.test", message.getFrom()[0].toString());
      assertTrue(message.getContent().toString().replace("\r\n", "\n").contains(body));
    } finally { pool.shutdownNow(); }
  }
}
