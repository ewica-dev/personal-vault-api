package com.ewicadev.personalvaultapi.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ewicadev.personalvaultapi.entity.EmailVerificationToken;

import jakarta.persistence.LockModeType;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

  List<EmailVerificationToken> findByUserId(Long userId);

  @Query("SELECT t FROM EmailVerificationToken t WHERE t.user.id = :userId AND t.consumedAt IS NULL AND t.expiresAt > CURRENT_TIMESTAMP AND t.attemptCount < 5 AND t.user.emailVerified = false ORDER BY t.createdAt DESC")
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<EmailVerificationToken> findActiveTokenByUserIdWithLock(@Param("userId") Long userId);

  @Modifying
  @Query("UPDATE EmailVerificationToken t SET t.consumedAt = CURRENT_TIMESTAMP WHERE t.user.id = :userId AND t.id <> :excludeId AND t.consumedAt IS NULL")
  int consumeAllByUserIdExcept(@Param("userId") Long userId, @Param("excludeId") Long excludeId);

  @Modifying
  @Query("DELETE FROM EmailVerificationToken t WHERE t.consumedAt IS NOT NULL OR (t.expiresAt < CURRENT_TIMESTAMP AND t.createdAt < :cutoff)")
  int deleteExpiredOrConsumedOlderThan(@Param("cutoff") LocalDateTime cutoff);
}