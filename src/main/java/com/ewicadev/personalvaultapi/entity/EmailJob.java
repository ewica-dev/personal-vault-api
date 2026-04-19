package com.ewicadev.personalvaultapi.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "email_jobs", indexes = {
    @Index(name = "idx_email_jobs_status_next_retry", columnList = "status, next_retry_at"),
    @Index(name = "idx_email_jobs_status_claimed_at", columnList = "status, claimed_at")
})
@NoArgsConstructor
public class EmailJob {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 255)
  private String email;

  @Column(length = 255)
  private String otpCode;

  private Instant expiresAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EmailJobStatus status;

  @Column(nullable = false)
  private Integer attemptCount = 0;

  private Instant nextRetryAt;

  private Instant claimedAt;

  @Column(length = 100)
  private String claimedBy;

  @Enumerated(EnumType.STRING)
  private FailureType failureType;

  @Column(length = 100)
  private String lastErrorCode;

  @Column(length = 500)
  private String lastErrorMessage;

  @Column(nullable = false)
  private Instant createdAt;

  private Instant sentAt;

  @PrePersist
  protected void onCreate() {
    if (this.createdAt == null) {
      this.createdAt = Instant.now();
    }
    if (this.attemptCount == null) {
      this.attemptCount = 0;
    }
  }
}