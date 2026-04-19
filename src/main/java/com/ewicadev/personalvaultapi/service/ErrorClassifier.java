package com.ewicadev.personalvaultapi.service;

import com.ewicadev.personalvaultapi.entity.FailureType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class ErrorClassifier {

  public FailureType classify(Throwable cause) {
    if (cause == null) {
      return FailureType.PERMANENT;
    }

    String message = cause.getMessage();
    if (message == null) {
      message = cause.getClass().getSimpleName();
    }
    message = message.toLowerCase();

    // Connection/socket issues - RETRYABLE
    if (message.contains("connection") ||
        message.contains("timeout") ||
        message.contains("socket") ||
        message.contains("unreachable") ||
        message.contains("refused") ||
        message.contains("network")) {
      return FailureType.RETRYABLE;
    }

    // SMTP relay temp failures - RETRYABLE
    if (message.contains("temporary") ||
        message.contains("429") ||
        message.contains("450") ||
        message.contains("451")) {
      return FailureType.RETRYABLE;
    }

    // 4xx protocol - likely RETRYABLE but worth trying
    if (message.contains("429") ||
        message.contains("450") ||
        message.contains("451") ||
        message.contains("452")) {
      return FailureType.RETRYABLE;
    }

    // Invalid recipient, bad sender, auth, or permanent 5xx - PERMANENT
    if (message.contains("invalid") ||
        message.contains("recipient") ||
        message.contains("sender") ||
        message.contains("unauthorized") ||
        message.contains("authentication") ||
        message.contains("5") ||
        (message.contains("550") && !message.contains("temporary")) ||
        message.contains("553") ||
        message.contains("554")) {
      return FailureType.PERMANENT;
    }

    // Unknown - default to RETRYABLE to avoid losing emails
    return FailureType.RETRYABLE;
  }

  public String getErrorCode(Throwable cause) {
    if (cause == null) {
      return "UNKNOWN";
    }

    String message = cause.getMessage();
    if (message == null) {
      message = cause.getClass().getSimpleName();
    }

    // Connection issues
    if (message.toLowerCase().contains("connection refused")) {
      return "CONNECTION_REFUSED";
    }
    if (message.toLowerCase().contains("timeout")) {
      return "CONNECTION_TIMEOUT";
    }
    if (message.toLowerCase().contains("socket")) {
      return "SOCKET_ERROR";
    }

    // SMTP errors
    if (message.toLowerCase().contains("550")) {
      return "INVALID_RECIPIENT";
    }
    if (message.toLowerCase().contains("553")) {
      return "INVALID_SENDER";
    }
    if (message.toLowerCase().contains("554")) {
      return "PERMANENT_REJECTION";
    }
    if (message.toLowerCase().contains("authentication")) {
      return "AUTH_FAILURE";
    }
    if (message.toLowerCase().contains("unauthorized")) {
      return "SENDER_NOT_AUTHORIZED";
    }

    return "UNKNOWN_ERROR";
  }
}