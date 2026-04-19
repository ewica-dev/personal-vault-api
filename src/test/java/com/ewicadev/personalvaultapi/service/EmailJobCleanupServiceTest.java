package com.ewicadev.personalvaultapi.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ewicadev.personalvaultapi.entity.EmailJob;
import com.ewicadev.personalvaultapi.entity.EmailJobRepository;
import com.ewicadev.personalvaultapi.entity.EmailJobStatus;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@Import(EmailJobCleanupService.class)
@ActiveProfiles("test")
class EmailJobCleanupServiceTest {

  @Autowired
  private EmailJobRepository repository;

  @Autowired
  private EmailJobCleanupService cleanupService;

  @BeforeEach
  void setUp() {
    repository.deleteAll();
  }

  @Test
  @DisplayName("should delete SUBMITTED jobs older than 7 days but keep newer ones")
  void shouldDeleteOldSubmittedJobs() {
    Instant now = Instant.now();
    Instant eightDaysAgo = now.minus(Duration.ofDays(8));
    Instant twoDaysAgo = now.minus(Duration.ofDays(2));

    EmailJob oldSubmitted = new EmailJob();
    oldSubmitted.setEmail("old@example.com");
    oldSubmitted.setOtpCode("123456");
    oldSubmitted.setStatus(EmailJobStatus.SUBMITTED);
    oldSubmitted.setAttemptCount(1);
    oldSubmitted.setCreatedAt(eightDaysAgo);
    oldSubmitted.setSentAt(eightDaysAgo);

    EmailJob recentSubmitted = new EmailJob();
    recentSubmitted.setEmail("recent@example.com");
    recentSubmitted.setOtpCode("654321");
    recentSubmitted.setStatus(EmailJobStatus.SUBMITTED);
    recentSubmitted.setAttemptCount(1);
    recentSubmitted.setCreatedAt(twoDaysAgo);
    recentSubmitted.setSentAt(twoDaysAgo);

    repository.saveAll(java.util.List.of(oldSubmitted, recentSubmitted));

    // Execute cleanup
    cleanupService.cleanupOldJobs();

    // Assertions
    assertThat(repository.count()).isEqualTo(1);
    assertThat(repository.findById(oldSubmitted.getId())).isEmpty();
    assertThat(repository.findById(recentSubmitted.getId())).isPresent();
  }

  @Test
  @DisplayName("should delete PERMANENTLY_FAILED jobs older than 7 days but keep newer ones")
  void shouldDeleteOldPermanentlyFailedJobs() {
    Instant now = Instant.now();
    Instant eightDaysAgo = now.minus(Duration.ofDays(8));
    Instant twoDaysAgo = now.minus(Duration.ofDays(2));

    EmailJob oldFailed = new EmailJob();
    oldFailed.setEmail("old@example.com");
    oldFailed.setOtpCode("123456");
    oldFailed.setStatus(EmailJobStatus.PERMANENTLY_FAILED);
    oldFailed.setAttemptCount(3);
    oldFailed.setCreatedAt(eightDaysAgo);
    oldFailed.setSentAt(null);

    EmailJob recentFailed = new EmailJob();
    recentFailed.setEmail("recent@example.com");
    recentFailed.setOtpCode("654321");
    recentFailed.setStatus(EmailJobStatus.PERMANENTLY_FAILED);
    recentFailed.setAttemptCount(3);
    recentFailed.setCreatedAt(twoDaysAgo);
    recentFailed.setSentAt(null);

    repository.saveAll(java.util.List.of(oldFailed, recentFailed));

    cleanupService.cleanupOldJobs();

    assertThat(repository.count()).isEqualTo(1);
    assertThat(repository.findById(oldFailed.getId())).isEmpty();
    assertThat(repository.findById(recentFailed.getId())).isPresent();
  }

  @Test
  @DisplayName("should delete non-terminal jobs older than 24 hours but keep newer ones")
  void shouldDeleteOldNonTerminalJobs() {
    Instant now = Instant.now();
    Instant twentyFiveHoursAgo = now.minus(Duration.ofHours(25));
    Instant twentyThreeHoursAgo = now.minus(Duration.ofHours(23));

    EmailJob oldPending = new EmailJob();
    oldPending.setEmail("old@example.com");
    oldPending.setOtpCode("123456");
    oldPending.setStatus(EmailJobStatus.PENDING);
    oldPending.setAttemptCount(0);
    oldPending.setCreatedAt(twentyFiveHoursAgo);
    oldPending.setSentAt(null);

    EmailJob recentPending = new EmailJob();
    recentPending.setEmail("recent@example.com");
    recentPending.setOtpCode("654321");
    recentPending.setStatus(EmailJobStatus.PENDING);
    recentPending.setAttemptCount(0);
    recentPending.setCreatedAt(twentyThreeHoursAgo);
    recentPending.setSentAt(null);

    repository.saveAll(java.util.List.of(oldPending, recentPending));

    cleanupService.cleanupOldJobs();

    assertThat(repository.count()).isEqualTo(1);
    assertThat(repository.findById(oldPending.getId())).isEmpty();
    assertThat(repository.findById(recentPending.getId())).isPresent();
  }

  @Test
  @DisplayName("should delete PROCESSING jobs older than 24 hours")
  void shouldDeleteOldProcessingJobs() {
    Instant now = Instant.now();
    Instant twentyFiveHoursAgo = now.minus(Duration.ofHours(25));

    EmailJob oldProcessing = new EmailJob();
    oldProcessing.setEmail("old@example.com");
    oldProcessing.setOtpCode("123456");
    oldProcessing.setStatus(EmailJobStatus.PROCESSING);
    oldProcessing.setAttemptCount(0);
    oldProcessing.setCreatedAt(twentyFiveHoursAgo);
    oldProcessing.setSentAt(null);

    repository.save(oldProcessing);

    cleanupService.cleanupOldJobs();

    assertThat(repository.count()).isZero();
  }

  @Test
  @DisplayName("should delete RETRY_SCHEDULED jobs older than 24 hours")
  void shouldDeleteOldRetryScheduledJobs() {
    Instant now = Instant.now();
    Instant twentyFiveHoursAgo = now.minus(Duration.ofHours(25));

    EmailJob oldRetry = new EmailJob();
    oldRetry.setEmail("old@example.com");
    oldRetry.setOtpCode("123456");
    oldRetry.setStatus(EmailJobStatus.RETRY_SCHEDULED);
    oldRetry.setAttemptCount(1);
    oldRetry.setCreatedAt(twentyFiveHoursAgo);
    oldRetry.setNextRetryAt(now.minus(Duration.ofHours(1)));

    repository.save(oldRetry);

    cleanupService.cleanupOldJobs();

    assertThat(repository.count()).isZero();
  }
}
