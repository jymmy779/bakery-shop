package com.bakery.account.repository;

import com.bakery.account.entity.PasswordResetOtp;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {
}
