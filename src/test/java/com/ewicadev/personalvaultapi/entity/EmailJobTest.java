package com.ewicadev.personalvaultapi.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EmailJobTest {

  @Nested
  @DisplayName("EmailJobStatus enum tests")
  class EmailJobStatusTests {

    @Test
    @DisplayName("should have all expected status values")
    void shouldHaveAllExpectedValues() {
      assertEquals(5, EmailJobStatus.values().length);
      assertNotNull(EmailJobStatus.PENDING);
      assertNotNull(EmailJobStatus.PROCESSING);
      assertNotNull(EmailJobStatus.RETRY_SCHEDULED);
      assertNotNull(EmailJobStatus.SUBMITTED);
      assertNotNull(EmailJobStatus.PERMANENTLY_FAILED);
    }

    @Test
    @DisplayName("should convert to string correctly")
    void shouldConvertToStringCorrectly() {
      assertEquals("PENDING", EmailJobStatus.PENDING.toString());
      assertEquals("SUBMITTED", EmailJobStatus.SUBMITTED.toString());
    }
  }

  @Nested
  @DisplayName("FailureType enum tests")
  class FailureTypeTests {

    @Test
    @DisplayName("should have all expected values")
    void shouldHaveAllExpectedValues() {
      assertEquals(2, FailureType.values().length);
      assertNotNull(FailureType.RETRYABLE);
      assertNotNull(FailureType.PERMANENT);
    }

    @Test
    @DisplayName("should convert to string correctly")
    void shouldConvertToStringCorrectly() {
      assertEquals("RETRYABLE", FailureType.RETRYABLE.toString());
      assertEquals("PERMANENT", FailureType.PERMANENT.toString());
    }
  }

  @Nested
  @DisplayName("EmailJob entity tests")
  class EmailJobEntityTests {

    @Test
    @DisplayName("should create EmailJob with default values")
    void shouldCreateWithDefaultValues() {
      EmailJob job = new EmailJob();
      assertNotNull(job);
      assertEquals(0, job.getAttemptCount());
    }

    @Test
    @DisplayName("should set and get all fields")
    void shouldSetAndGetAllFields() {
      EmailJob job = new EmailJob();
      Instant now = Instant.now();

      job.setId(1L);
      job.setEmail("test@example.com");
      job.setOtpCode("123456");
      job.setExpiresAt(now);
      job.setStatus(EmailJobStatus.PENDING);
      job.setAttemptCount(0);
      job.setNextRetryAt(now);
      job.setClaimedAt(now);
      job.setClaimedBy("worker-1");
      job.setFailureType(FailureType.RETRYABLE);
      job.setLastErrorCode("ERR_001");
      job.setLastErrorMessage("Test error");
      job.setCreatedAt(now);
      job.setSentAt(now);

      assertEquals(1L, job.getId());
      assertEquals("test@example.com", job.getEmail());
      assertEquals("123456", job.getOtpCode());
      assertEquals(now, job.getExpiresAt());
      assertEquals(EmailJobStatus.PENDING, job.getStatus());
      assertEquals(0, job.getAttemptCount());
      assertEquals(now, job.getNextRetryAt());
      assertEquals(now, job.getClaimedAt());
      assertEquals("worker-1", job.getClaimedBy());
      assertEquals(FailureType.RETRYABLE, job.getFailureType());
      assertEquals("ERR_001", job.getLastErrorCode());
      assertEquals("Test error", job.getLastErrorMessage());
      assertEquals(now, job.getCreatedAt());
      assertEquals(now, job.getSentAt());
    }

    @Test
    @DisplayName("should allow null otpCode for security scrubbing")
    void shouldAllowNullOtpCode() {
      EmailJob job = new EmailJob();
      job.setOtpCode(null);

      assertTrue(job.getOtpCode() == null);
    }

    @Test
    @DisplayName("should auto-set createdAt on prePersist")
    void shouldAutoSetCreatedAt() {
      EmailJob job = new EmailJob();
      job.onCreate();

      assertNotNull(job.getCreatedAt());
    }
  }
}