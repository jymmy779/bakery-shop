package com.bakery.notification;

import com.bakery.account.entity.Account;
import com.bakery.order.entity.Order;

public interface EmailService {

  void sendRegistrationConfirmation(Account account);

  void sendPasswordResetOtp(Account account, String otp);

  void sendOrderConfirmation(Order order);
}
