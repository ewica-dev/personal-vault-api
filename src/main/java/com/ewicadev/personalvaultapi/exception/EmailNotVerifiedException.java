package com.ewicadev.personalvaultapi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class EmailNotVerifiedException extends RuntimeException {

  public EmailNotVerifiedException() {
    super("Email not verified. Please verify your email to login.");
  }
}