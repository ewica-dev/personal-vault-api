package com.ewicadev.personalvaultapi.service;

import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ewicadev.personalvaultapi.entity.EmailJob;
import com.ewicadev.personalvaultapi.entity.EmailJobRepository;
import com.ewicadev.personalvaultapi.entity.EmailJobStatus;
import com.ewicadev.personalvaultapi.entity.FailureType;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;

@Service
public class EmailJobWorker {

  private static final Logger log = LoggerFactory.getLogger(EmailJobWorker.class);

  private static final int MAX_ATTEMPTS = 3;
  private static final int BATCH_SIZE = 50;
  private static final Duration SMTP_TIMEOUT = Duration.ofSeconds(30);
  private static final Duration RECLAIM_THRESHOLD = Duration.ofMinutes(5);

  private static final Duration[] BACKOFF_SCHEDULE = {
      Duration.ofMinutes(2),
      Duration.ofMinutes(4)
  };

  private final EmailJobRepository repository;
  private final JavaMailSender mailSender;
  private final ErrorClassifier errorClassifier;
  private final MeterRegistry meterRegistry;
  private String workerInstanceId;

  private Counter submittedCounter;
  private Counter retryableFailureCounter;
  private Counter permanentFailureCounter;
  private Counter claimConflictCounter;
  private Counter reclaimTriggeredCounter;
  private Timer sendLatencyTimer;
  private Timer queueDelayTimer;

  public EmailJobWorker(
      EmailJobRepository repository,
      JavaMailSender mailSender,
      ErrorClassifier errorClassifier,
      MeterRegistry meterRegistry) {
    this.repository = repository;
    this.mailSender = mailSender;
    this.errorClassifier = errorClassifier;
    this.meterRegistry = meterRegistry;
  }

  @PostConstruct
  public void init() {
    try {
      String hostname = InetAddress.getLocalHost().getHostName();
      workerInstanceId = hostname + "-" + UUID.randomUUID().toString().substring(0, 8);
    } catch (Exception e) {
      workerInstanceId = "unknown-" + UUID.randomUUID().toString().substring(0, 8);
    }
    log.info("EmailJobWorker initialized with instance ID: {}", workerInstanceId);

    // Initialize metrics
    submittedCounter = meterRegistry.counter("email.jobs.submitted");
    retryableFailureCounter = meterRegistry.counter("email.jobs.retryable_failure");
    permanentFailureCounter = meterRegistry.counter("email.jobs.permanent_failure");
    claimConflictCounter = meterRegistry.counter("email.jobs.claim_conflicts");
    reclaimTriggeredCounter = meterRegistry.counter("email.jobs.reclaim_triggered");
    sendLatencyTimer = meterRegistry.timer("email.send.latency");
    queueDelayTimer = meterRegistry.timer("email.jobs.queue_delay");
  }

  @Transactional
  @Scheduled(fixedDelayString = "${email.worker.poll-interval:5000}")
  public void processEmailJobs() {
    Instant now = Instant.now();

    List<EmailJob> candidates = repository.findDueJobs(
        EmailJobStatus.PENDING,
        EmailJobStatus.RETRY_SCHEDULED,
        now,
        org.springframework.data.domain.PageRequest.of(0, BATCH_SIZE)
    );

    for (EmailJob job : candidates) {
      int claimed = repository.claimJob(
          job.getId(),
          EmailJobStatus.PENDING,
          EmailJobStatus.RETRY_SCHEDULED,
          EmailJobStatus.PROCESSING,
          now,
          workerInstanceId
      );

      if (claimed == 1) {
        // CRITICAL: Use job from query list directly - do NOT re-fetch
        // This avoids Hibernate L1 cache stale entity bug
        processJob(job);
      } else {
        // Count claim conflicts (another worker beat us to it)
        claimConflictCounter.increment();
      }
    }
  }

  public void processJob(EmailJob job) {
    try {
      // Record queue delay (time from creation to processing start)
      queueDelayTimer.record(Duration.between(job.getCreatedAt(), Instant.now()));

      String email = job.getEmail();
      String otpCode = job.getOtpCode();

      if (otpCode == null || email == null) {
        log.warn("Job {} has null email or otpCode, marking permanent failure", job.getId());
        permanentFailureCounter.increment();
        repository.markPermanentFailure(
            job.getId(),
            EmailJobStatus.PERMANENTLY_FAILED,
            EmailJobStatus.PROCESSING,
            FailureType.PERMANENT,
            "MISSING_PAYLOAD",
            "Email or OTP was null"
        );
        return;
      }

      // Send email
      SimpleMailMessage message = new SimpleMailMessage();
      message.setTo(email);
      message.setSubject("Your Personal Vault Verification Code");
      message.setText("Your verification code is: " + otpCode + "\n\nThis code expires in 10 minutes.");
      mailSender.send(message);

      // Mark as submitted
      Instant now = Instant.now();
      repository.markSubmitted(
          job.getId(),
          EmailJobStatus.SUBMITTED,
          EmailJobStatus.PROCESSING,
          now
      );

      // Record metrics
      submittedCounter.increment();
      sendLatencyTimer.record(Duration.between(job.getCreatedAt(), now));

      log.info("Email sent successfully to {}", email);

    } catch (MailException e) {
      handleFailure(job, e);
    } catch (Exception e) {
      handleFailure(job, e);
    }
  }

  private void handleFailure(EmailJob job, Exception e) {
    FailureType failureType = errorClassifier.classify(e);
    String errorCode = errorClassifier.getErrorCode(e);
    String errorMessage = sanitizeErrorMessage(e.getMessage());

    Instant now = Instant.now();
    int attempt = job.getAttemptCount() + 1;

    // Check terminal conditions
    boolean isPermanent = failureType == FailureType.PERMANENT;
    boolean exceededMaxAttempts = attempt >= MAX_ATTEMPTS;

    // Check if enough time remains for next retry
    Duration timeRemaining = null;
    boolean insufficientTimeForRetry = false;

    if (job.getExpiresAt() != null) {
      timeRemaining = Duration.between(now, job.getExpiresAt());
      int backoffIndex = Math.min(attempt - 1, BACKOFF_SCHEDULE.length - 1);
      Duration nextDelay = BACKOFF_SCHEDULE[backoffIndex];
      insufficientTimeForRetry = timeRemaining.isNegative() ||
          timeRemaining.compareTo(nextDelay) < 0;
    }

    if (isPermanent || exceededMaxAttempts || insufficientTimeForRetry) {
      repository.markPermanentFailure(
          job.getId(),
          EmailJobStatus.PERMANENTLY_FAILED,
          EmailJobStatus.PROCESSING,
          failureType,
          errorCode,
          errorMessage
      );
      permanentFailureCounter.increment();
      log.error("Job {} marked permanent failure: attempts={}, isPermanent={}, exceededMax={}, insufficientTime={}",
          job.getId(), attempt, isPermanent, exceededMaxAttempts, insufficientTimeForRetry);
    } else {
      // Schedule retry
      int backoffIndex = Math.min(attempt - 1, BACKOFF_SCHEDULE.length - 1);
      Duration delay = BACKOFF_SCHEDULE[backoffIndex];
      Instant nextRetryAt = now.plus(delay);

      repository.scheduleRetry(
          job.getId(),
          EmailJobStatus.RETRY_SCHEDULED,
          EmailJobStatus.PROCESSING,
          nextRetryAt,
          failureType,
          errorCode,
          errorMessage
      );

      retryableFailureCounter.increment();
      log.warn("Job {} scheduled for retry at {}: attempt={}, error={}",
          job.getId(), nextRetryAt, attempt, errorCode);
    }

    // Record queue delay timer (time from creation to failure/retry)
    queueDelayTimer.record(Duration.between(job.getCreatedAt(), now));
  }

  private String sanitizeErrorMessage(String message) {
    if (message == null) {
      return "Unknown error";
    }
    // Truncate to 500 chars, remove newlines
    message = message.replace("\n", " ").replace("\r", " ");
    if (message.length() > 500) {
      message = message.substring(0, 500);
    }
    return message;
  }

  @Transactional
  @Scheduled(fixedDelayString = "${email.worker.reclaim-interval:60000}")
  public void reclaimStuckJobs() {
    Instant now = Instant.now();
    Instant stuckThreshold = now.minus(RECLAIM_THRESHOLD);

    int reclaimed = repository.reclaimStuckJobs(
        EmailJobStatus.PROCESSING,
        EmailJobStatus.RETRY_SCHEDULED,
        now,
        stuckThreshold
    );

    if (reclaimed > 0) {
      reclaimTriggeredCounter.increment(reclaimed);
      log.info("Reclaimed {} stuck jobs", reclaimed);
    }
  }
}