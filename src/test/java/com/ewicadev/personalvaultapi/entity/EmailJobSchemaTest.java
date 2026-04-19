package com.ewicadev.personalvaultapi.entity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class EmailJobSchemaTest {

  @Autowired
  private EmailJobRepository repository;

  @Test
  @DisplayName("should save and retrieve EmailJob from database")
  void shouldSaveAndRetrieveEmailJob() {
    EmailJob job = new EmailJob();
    job.setEmail("test@example.com");
    job.setOtpCode("123456");
    job.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
    job.setStatus(EmailJobStatus.PENDING);
    job.setAttemptCount(0);
    job.setCreatedAt(Instant.now());

    EmailJob saved = repository.saveAndFlush(job);

    assertNotNull(saved.getId());
    assertThat(saved.getEmail()).isEqualTo("test@example.com");
    assertThat(saved.getOtpCode()).isEqualTo("123456");
    assertThat(saved.getStatus()).isEqualTo(EmailJobStatus.PENDING);
    assertThat(saved.getAttemptCount()).isEqualTo(0);
  }

  @Test
  @DisplayName("should auto-set createdAt on persist")
  void shouldAutoSetCreatedAt() {
    EmailJob job = new EmailJob();
    job.setEmail("test@example.com");
    job.setStatus(EmailJobStatus.PENDING);
    job.setAttemptCount(0);

    EmailJob saved = repository.saveAndFlush(job);

    assertNotNull(saved.getCreatedAt());
  }

  @Test
  @DisplayName("should allow null otpCode after terminal state")
  void shouldAllowNullOtpCode() {
    EmailJob job = new EmailJob();
    job.setEmail("test@example.com");
    job.setOtpCode(null);
    job.setStatus(EmailJobStatus.SUBMITTED);
    job.setAttemptCount(1);
    job.setCreatedAt(Instant.now());
    job.setSentAt(Instant.now());

    EmailJob saved = repository.saveAndFlush(job);

    assertThat(saved.getOtpCode()).isNull();
  }
}