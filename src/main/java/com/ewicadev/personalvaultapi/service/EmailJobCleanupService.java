package com.ewicadev.personalvaultapi.service;

import com.ewicadev.personalvaultapi.entity.EmailJobRepository;
import com.ewicadev.personalvaultapi.entity.EmailJobStatus;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailJobCleanupService {

  private static final Logger log = LoggerFactory.getLogger(EmailJobCleanupService.class);

  private final EmailJobRepository repository;

  // Retention periods
  private static final Duration SUBMITTED_RETENTION = Duration.ofDays(7);
  private static final Duration PERMANENTLY_FAILED_RETENTION = Duration.ofDays(7);
  private static final Duration NON_TERMINAL_RETENTION = Duration.ofHours(24);

  public EmailJobCleanupService(EmailJobRepository repository) {
    this.repository = repository;
  }

  @Scheduled(cron = "0 0 3 * * ?")
  @Transactional
  public void cleanupOldJobs() {
    Instant now = Instant.now();
    log.info("Starting email job cleanup");

    // 1. Delete SUBMITTED jobs older than 7 days (by sentAt)
    Instant submittedCutoff = now.minus(SUBMITTED_RETENTION);
    int submittedDeleted = repository.deleteSubmittedOlderThan(EmailJobStatus.SUBMITTED, submittedCutoff);
    log.info("Deleted {} SUBMITTED jobs older than {}", submittedDeleted, submittedCutoff);

    // 2. Delete PERMANENTLY_FAILED jobs older than 7 days (by createdAt)
    Instant failedCutoff = now.minus(PERMANENTLY_FAILED_RETENTION);
    int failedDeleted = repository.deletePermanentlyFailedOlderThan(EmailJobStatus.PERMANENTLY_FAILED, failedCutoff);
    log.info("Deleted {} PERMANENTLY_FAILED jobs older than {}", failedDeleted, failedCutoff);

    // 3. Delete non-terminal jobs (PENDING, RETRY_SCHEDULED, PROCESSING) older than 24h (by createdAt)
    Instant nonTerminalCutoff = now.minus(NON_TERMINAL_RETENTION);
    int nonTerminalDeleted = repository.deleteNonTerminalOlderThan(
        java.util.List.of(EmailJobStatus.PENDING, EmailJobStatus.RETRY_SCHEDULED, EmailJobStatus.PROCESSING),
        nonTerminalCutoff
    );
    log.info("Deleted {} non-terminal jobs older than {}", nonTerminalDeleted, nonTerminalCutoff);

    int totalDeleted = submittedDeleted + failedDeleted + nonTerminalDeleted;
    log.info("Email job cleanup complete. Total deleted: {}", totalDeleted);
  }
}
