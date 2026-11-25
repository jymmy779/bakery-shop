package com.bakery.account.repository;

import com.bakery.account.entity.PasswordResetOtp;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {

  List<PasswordResetOtp> findByAccount_IdAndUsedAtIsNull(Long accountId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<PasswordResetOtp> findTopByAccount_IdAndUsedAtIsNullOrderByCreatedAtDesc(Long accountId);

  Optional<PasswordResetOtp> findTopByAccount_IdOrderByCreatedAtDesc(Long accountId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select otp from PasswordResetOtp otp where otp.id = :id")
  Optional<PasswordResetOtp> findByIdForUpdate(@Param("id") Long id);
}
