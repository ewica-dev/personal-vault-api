package com.ewicadev.personalvaultapi.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResendVerificationResponse {

  private String message;
}