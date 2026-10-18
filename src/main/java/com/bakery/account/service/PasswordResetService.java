package com.bakery.account.service;

import com.bakery.account.dto.ForgotPasswordRequest;
import com.bakery.account.dto.ResetPasswordRequest;
import com.bakery.account.dto.VerifyOtpRequest;

public interface PasswordResetService {

  void sendOtp(ForgotPasswordRequest request);

  String verifyOtp(VerifyOtpRequest request);

  void resetPassword(ResetPasswordRequest request);
}
