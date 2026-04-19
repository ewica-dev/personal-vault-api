package com.ewicadev.personalvaultapi.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.ewicadev.personalvaultapi.entity.EmailJob;
import com.ewicadev.personalvaultapi.entity.EmailJobRepository;
import com.ewicadev.personalvaultapi.entity.EmailJobStatus;
import com.ewicadev.personalvaultapi.entity.FailureType;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class EmailJobWorkerTest {

  @Mock
  private EmailJobRepository repository;

  @Mock
  private JavaMailSender mailSender;

  private ErrorClassifier errorClassifier;
  private EmailJobWorker worker;

  @BeforeEach
  void setUp() {
    errorClassifier = new ErrorClassifier();
    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    worker = new EmailJobWorker(repository, mailSender, errorClassifier, meterRegistry);
    worker.init();
  }

  @Test
  @DisplayName("should successfully send email and mark submitted")
  void shouldSendEmailSuccessfully() throws Exception {
    Instant now = Instant.now();

    EmailJob job = new EmailJob();
    job.setId(1L);
    job.setEmail("test@example.com");
    job.setOtpCode("123456");
    job.setStatus(EmailJobStatus.PENDING);
    job.setAttemptCount(0);
    job.setCreatedAt(now);

    when(repository.findDueJobs(eq(EmailJobStatus.PENDING), eq(EmailJobStatus.RETRY_SCHEDULED), any(Instant.class), any(PageRequest.class)))
        .thenReturn(java.util.List.of(job));

    when(repository.claimJob(eq(1L), eq(EmailJobStatus.PENDING), eq(EmailJobStatus.RETRY_SCHEDULED), eq(EmailJobStatus.PROCESSING), any(Instant.class), any(String.class)))
        .thenReturn(1);

    worker.processEmailJobs();

    verify(mailSender).send(any(SimpleMailMessage.class));
    verify(repository).markSubmitted(eq(1L), eq(EmailJobStatus.SUBMITTED), eq(EmailJobStatus.PROCESSING), any(Instant.class));
  }

  @Test
  @DisplayName("should schedule retry on timeout failure")
  void shouldScheduleRetryOnTimeout() throws Exception {
    Instant now = Instant.now();

    EmailJob job = new EmailJob();
    job.setId(1L);
    job.setEmail("test@example.com");
    job.setOtpCode("123456");
    job.setStatus(EmailJobStatus.PENDING);
    job.setAttemptCount(0);
    job.setCreatedAt(now);
    job.setExpiresAt(now.plus(10, ChronoUnit.MINUTES));

    when(repository.findDueJobs(eq(EmailJobStatus.PENDING), eq(EmailJobStatus.RETRY_SCHEDULED), any(Instant.class), any(PageRequest.class)))
        .thenReturn(java.util.List.of(job));

    when(repository.claimJob(eq(1L), eq(EmailJobStatus.PENDING), eq(EmailJobStatus.RETRY_SCHEDULED), eq(EmailJobStatus.PROCESSING), any(Instant.class), any(String.class)))
        .thenReturn(1);

    // Simulate timeout exception
    doThrow(new MailException("Connection timeout") {}).when(mailSender).send(any(SimpleMailMessage.class));

    worker.processEmailJobs();

    // Should schedule retry
    verify(repository).scheduleRetry(
        eq(1L),
        eq(EmailJobStatus.RETRY_SCHEDULED),
        eq(EmailJobStatus.PROCESSING),
        any(Instant.class),
        eq(FailureType.RETRYABLE),
        eq("CONNECTION_TIMEOUT"),
        any(String.class)
    );
  }

  @Test
  @DisplayName("should mark permanent failure after max attempts")
  void shouldMarkPermanentFailureAfterMaxAttempts() throws Exception {
    Instant now = Instant.now();

    EmailJob job = new EmailJob();
    job.setId(1L);
    job.setEmail("test@example.com");
    job.setOtpCode("123456");
    job.setStatus(EmailJobStatus.PENDING);
    job.setAttemptCount(2); // Already at max
    job.setCreatedAt(now);
    job.setExpiresAt(now.plus(10, ChronoUnit.MINUTES));

    when(repository.findDueJobs(eq(EmailJobStatus.PENDING), eq(EmailJobStatus.RETRY_SCHEDULED), any(Instant.class), any(PageRequest.class)))
        .thenReturn(java.util.List.of(job));

    when(repository.claimJob(eq(1L), eq(EmailJobStatus.PENDING), eq(EmailJobStatus.RETRY_SCHEDULED), eq(EmailJobStatus.PROCESSING), any(Instant.class), any(String.class)))
        .thenReturn(1);

    doThrow(new MailException("SMTP error") {}).when(mailSender).send(any(SimpleMailMessage.class));

    worker.processEmailJobs();

    // Should mark permanent failure, not retry
    verify(repository).markPermanentFailure(
        eq(1L),
        eq(EmailJobStatus.PERMANENTLY_FAILED),
        eq(EmailJobStatus.PROCESSING),
        any(FailureType.class),
        any(String.class),
        any(String.class)
    );
  }

  @Test
  @DisplayName("should not call findById in the loop - L1 cache bypass")
  void shouldNotReFetchInLoop() throws Exception {
    Instant now = Instant.now();

    EmailJob job = new EmailJob();
    job.setId(1L);
    job.setEmail("test@example.com");
    job.setOtpCode("123456");
    job.setStatus(EmailJobStatus.PENDING);
    job.setAttemptCount(0);
    job.setCreatedAt(now);

    when(repository.findDueJobs(any(), any(), any(Instant.class), any(PageRequest.class)))
        .thenReturn(java.util.List.of(job));

    when(repository.claimJob(eq(1L), any(), any(), any(), any(Instant.class), any(String.class)))
        .thenReturn(1);

    worker.processEmailJobs();

    // NEVER call findById - this is the L1 cache bypass test
    verify(repository, never()).findById(1L);
  }

  @Test
  @DisplayName("ErrorClassifier should classify timeout as RETRYABLE")
  void errorClassifierClassifiesTimeoutAsRetryable() {
    MailException timeoutException = new MailException("Connection timeout") {};

    FailureType type = errorClassifier.classify(timeoutException);

    assertEquals(FailureType.RETRYABLE, type);
  }

  @Test
  @DisplayName("ErrorClassifier should classify invalid recipient as PERMANENT")
  void errorClassifierClassifiesInvalidRecipientAsPermanent() {
    MailException invalidException = new MailException("550 Invalid recipient") {};

    FailureType type = errorClassifier.classify(invalidException);

    assertEquals(FailureType.PERMANENT, type);
  }

  @Test
  @DisplayName("ErrorClassifier should generate correct error codes")
  void errorClassifierGeneratesCorrectCodes() {
    MailException timeoutException = new MailException("Connection timeout") {};
    String code = errorClassifier.getErrorCode(timeoutException);

    assertEquals("CONNECTION_TIMEOUT", code);
  }

  @Test
  @DisplayName("worker should have instance ID")
  void workerShouldHaveInstanceId() {
    assertNotNull(worker.toString());
  }
}