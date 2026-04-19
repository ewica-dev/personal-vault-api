package com.ewicadev.personalvaultapi.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VerifyEmailResponse {

  private String message;
  private boolean verified;
}