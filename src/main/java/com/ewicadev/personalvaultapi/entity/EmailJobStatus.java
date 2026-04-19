package com.ewicadev.personalvaultapi.entity;

public enum EmailJobStatus {
  PENDING,
  PROCESSING,
  RETRY_SCHEDULED,
  SUBMITTED,
  PERMANENTLY_FAILED
}