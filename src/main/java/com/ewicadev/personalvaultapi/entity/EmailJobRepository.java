package com.ewicadev.personalvaultapi.entity;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailJobRepository extends JpaRepository<EmailJob, Long> {

  @Query("SELECT j FROM EmailJob j WHERE " +
         "(j.status = :pending AND j.nextRetryAt IS NULL) OR " +
         "(j.status = :retryScheduled AND j.nextRetryAt <= :now) " +
         "ORDER BY " +
         "CASE WHEN j.status = :pending THEN j.createdAt ELSE j.nextRetryAt END ASC, " +
         "j.id ASC")
  List<EmailJob> findDueJobs(
      @Param("pending") EmailJobStatus pending,
      @Param("retryScheduled") EmailJobStatus retryScheduled,
      @Param("now") Instant now,
      Pageable pageable);

  @Modifying(clearAutomatically = true)
  @Query("UPDATE EmailJob j SET j.status = :newStatus, j.claimedAt = :now, j.claimedBy = :workerId " +
         "WHERE j.id = :id " +
         "AND ((j.status = :pending AND j.nextRetryAt IS NULL) OR (j.status = :retryScheduled AND j.nextRetryAt <= :now))")
  int claimJob(
      @Param("id") Long id,
      @Param("pending") EmailJobStatus pending,
      @Param("retryScheduled") EmailJobStatus retryScheduled,
      @Param("newStatus") EmailJobStatus newStatus,
      @Param("now") Instant now,
      @Param("workerId") String workerId);

  @Modifying(clearAutomatically = true)
  @Query("UPDATE EmailJob j SET j.status = :submitted, j.sentAt = :now, j.claimedAt = NULL, j.claimedBy = NULL, " +
         "j.attemptCount = j.attemptCount + 1, j.otpCode = NULL " +
         "WHERE j.id = :id AND j.status = :processing")
  int markSubmitted(
      @Param("id") Long id,
      @Param("submitted") EmailJobStatus submitted,
      @Param("processing") EmailJobStatus processing,
      @Param("now") Instant now);

  @Modifying(clearAutomatically = true)
  @Query("UPDATE EmailJob j SET j.status = :scheduled, j.nextRetryAt = :nextRetryAt, " +
         "j.attemptCount = j.attemptCount + 1, j.failureType = :failureType, " +
         "j.lastErrorCode = :errorCode, j.lastErrorMessage = :errorMessage, " +
         "j.claimedAt = NULL, j.claimedBy = NULL " +
         "WHERE j.id = :id AND j.status = :processing")
  int scheduleRetry(
      @Param("id") Long id,
      @Param("scheduled") EmailJobStatus scheduled,
      @Param("processing") EmailJobStatus processing,
      @Param("nextRetryAt") Instant nextRetryAt,
      @Param("failureType") FailureType failureType,
      @Param("errorCode") String errorCode,
      @Param("errorMessage") String errorMessage);

  @Modifying(clearAutomatically = true)
  @Query("UPDATE EmailJob j SET j.status = :permanent, j.attemptCount = j.attemptCount + 1, " +
         "j.failureType = :failureType, j.lastErrorCode = :errorCode, " +
         "j.lastErrorMessage = :errorMessage, j.claimedAt = NULL, j.claimedBy = NULL, j.otpCode = NULL " +
         "WHERE j.id = :id AND j.status = :processing")
  int markPermanentFailure(
      @Param("id") Long id,
      @Param("permanent") EmailJobStatus permanent,
      @Param("processing") EmailJobStatus processing,
      @Param("failureType") FailureType failureType,
      @Param("errorCode") String errorCode,
      @Param("errorMessage") String errorMessage);

  @Modifying(clearAutomatically = true)
  @Query("UPDATE EmailJob j SET j.status = :scheduled, j.nextRetryAt = :now, " +
         "j.claimedAt = NULL, j.claimedBy = NULL, j.attemptCount = j.attemptCount + 1 " +
         "WHERE j.status = :processing AND j.claimedAt < :stuckThreshold")
  int reclaimStuckJobs(
      @Param("processing") EmailJobStatus processing,
      @Param("scheduled") EmailJobStatus scheduled,
      @Param("now") Instant now,
      @Param("stuckThreshold") Instant stuckThreshold);

  @Modifying(clearAutomatically = true)
  @Query("DELETE FROM EmailJob WHERE id IN :ids AND status = :status")
  int deleteByIdsAndStatusIn(@Param("ids") List<Long> ids, @Param("status") EmailJobStatus status);

  @Modifying(clearAutomatically = true)
  @Query("DELETE FROM EmailJob WHERE status = :status AND sentAt < :cutoff")
  int deleteSubmittedOlderThan(@Param("status") EmailJobStatus status, @Param("cutoff") Instant cutoff);

  @Modifying(clearAutomatically = true)
  @Query("DELETE FROM EmailJob WHERE status = :status AND createdAt < :cutoff")
  int deletePermanentlyFailedOlderThan(@Param("status") EmailJobStatus status, @Param("cutoff") Instant cutoff);

  @Modifying(clearAutomatically = true)
  @Query("DELETE FROM EmailJob WHERE status IN (:statuses) AND createdAt < :cutoff")
  int deleteNonTerminalOlderThan(@Param("statuses") List<EmailJobStatus> statuses, @Param("cutoff") Instant cutoff);
}