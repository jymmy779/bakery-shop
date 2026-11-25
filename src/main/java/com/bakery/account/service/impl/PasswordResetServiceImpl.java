package com.bakery.account.service.impl;

import com.bakery.account.dto.ForgotPasswordRequest;
import com.bakery.account.dto.ResetPasswordRequest;
import com.bakery.account.dto.VerifyOtpRequest;
import com.bakery.account.entity.Account;
import com.bakery.account.entity.AccountStatus;
import com.bakery.account.entity.PasswordResetOtp;
import com.bakery.account.repository.AccountRepository;
import com.bakery.account.repository.PasswordResetOtpRepository;
import com.bakery.account.service.PasswordResetService;
import com.bakery.common.exception.InvalidOperationException;
import com.bakery.notification.EmailService;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

  private static final int OTP_LIFETIME_MINUTES = 10;
  private static final int RESET_TOKEN_LIFETIME_MINUTES = 10;
  private static final int MAX_FAILED_ATTEMPTS = 5;
  private static final int RESEND_COOLDOWN_SECONDS = 60;

  private final AccountRepository accountRepository;
  private final PasswordResetOtpRepository otpRepository;
  private final PasswordEncoder passwordEncoder;
  private final EmailService emailService;
  private final SecureRandom secureRandom = new SecureRandom();

  @Override
  @Transactional
  public void sendOtp(ForgotPasswordRequest request) {
    String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
    accountRepository.findByEmailIgnoreCase(email)
        .filter(account -> account.getStatus() == AccountStatus.ACTIVE)
        .ifPresent(account -> createAndSendOtp(account));
  }

  @Override
  @Transactional(noRollbackFor = InvalidOperationException.class)
  public String verifyOtp(VerifyOtpRequest request) {
    Account account = accountRepository.findByEmailIgnoreCase(
            request.getEmail().trim().toLowerCase(Locale.ROOT))
        .filter(candidate -> candidate.getStatus() == AccountStatus.ACTIVE)
        .orElseThrow(this::invalidOtp);

    PasswordResetOtp otp = otpRepository
        .findTopByAccount_IdAndUsedAtIsNullOrderByCreatedAtDesc(account.getId())
        .orElseThrow(this::invalidOtp);
    LocalDateTime now = LocalDateTime.now();
    if (!otp.getExpiresAt().isAfter(now) || otp.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
      otp.setUsedAt(now);
      throw invalidOtp();
    }

    if (!passwordEncoder.matches(request.getOtp(), otp.getOtpHash())) {
      otp.setFailedAttempts(otp.getFailedAttempts() + 1);
      if (otp.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
        otp.setUsedAt(now);
      }
      throw invalidOtp();
    }

    otp.setUsedAt(now);
    String secret = newSecret();
    // Reuse the OTP table row as a short-lived, one-use reset grant; only its secret hash is stored.
    PasswordResetOtp resetGrant = new PasswordResetOtp();
    resetGrant.setAccount(account);
    resetGrant.setOtpHash(passwordEncoder.encode(secret));
    resetGrant.setExpiresAt(now.plusMinutes(RESET_TOKEN_LIFETIME_MINUTES));
    resetGrant.setFailedAttempts(0);
    resetGrant = otpRepository.save(resetGrant);
    return resetGrant.getId() + "." + secret;
  }

  @Override
  @Transactional
  public void resetPassword(ResetPasswordRequest request) {
    if (!request.getNewPassword().equals(request.getConfirmPassword())) {
      throw new InvalidOperationException("Mật khẩu xác nhận không khớp");
    }

    String[] tokenParts = request.getResetToken().split("\\.", 2);
    if (tokenParts.length != 2) {
      throw invalidResetToken();
    }

    Long grantId;
    try {
      grantId = Long.valueOf(tokenParts[0]);
    } catch (NumberFormatException exception) {
      throw invalidResetToken();
    }

    PasswordResetOtp grant = otpRepository.findByIdForUpdate(grantId)
        .orElseThrow(this::invalidResetToken);
    LocalDateTime now = LocalDateTime.now();
    if (grant.getUsedAt() != null || !grant.getExpiresAt().isAfter(now)
        || !passwordEncoder.matches(tokenParts[1], grant.getOtpHash())) {
      throw invalidResetToken();
    }

    Account account = grant.getAccount();
    if (account.getStatus() != AccountStatus.ACTIVE) {
      throw invalidResetToken();
    }
    account.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
    for (PasswordResetOtp pending : otpRepository.findByAccount_IdAndUsedAtIsNull(account.getId())) {
      pending.setUsedAt(now);
    }
  }

  private void createAndSendOtp(Account account) {
    LocalDateTime now = LocalDateTime.now();
    PasswordResetOtp latest = otpRepository.findTopByAccount_IdOrderByCreatedAtDesc(account.getId())
        .orElse(null);
    if (latest != null && latest.getCreatedAt() != null
        && latest.getCreatedAt().plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(now)) {
      return;
    }

    for (PasswordResetOtp pending : otpRepository.findByAccount_IdAndUsedAtIsNull(account.getId())) {
      pending.setUsedAt(now);
    }

    String otpValue = String.format(Locale.ROOT, "%06d", secureRandom.nextInt(1_000_000));
    PasswordResetOtp otp = new PasswordResetOtp();
    otp.setAccount(account);
    otp.setOtpHash(passwordEncoder.encode(otpValue));
    otp.setExpiresAt(now.plusMinutes(OTP_LIFETIME_MINUTES));
    otp.setFailedAttempts(0);
    otpRepository.save(otp);
    emailService.sendPasswordResetOtp(account, otpValue);
  }

  private String newSecret() {
    byte[] bytes = new byte[32];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private InvalidOperationException invalidOtp() {
    return new InvalidOperationException("Mã OTP không đúng, đã hết hạn hoặc không còn hiệu lực");
  }

  private InvalidOperationException invalidResetToken() {
    return new InvalidOperationException("Phiên đặt lại mật khẩu không hợp lệ hoặc đã hết hạn");
  }
}
