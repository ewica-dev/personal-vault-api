package com.ewicadev.personalvaultapi.dto.auth;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SignupResponse {

  private String message;
  private UserResponse user;

  public SignupResponse(String message, Long id, String email, LocalDateTime createdAt, boolean emailVerified) {
    this.message = message;
    this.user = new UserResponse(id, email, createdAt, emailVerified);
  }

  @Data
  @AllArgsConstructor
  public static class UserResponse {
    private Long id;
    private String email;
    private LocalDateTime createdAt;
    private boolean emailVerified;
  }
}
