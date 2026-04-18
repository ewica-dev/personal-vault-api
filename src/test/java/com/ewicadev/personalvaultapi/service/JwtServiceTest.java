package com.ewicadev.personalvaultapi.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  private JwtService jwtService;
  private static final String SECRET = "mySecretKey12345678901234567890123456789012345678901234567890";
  private static final long EXPIRATION_MS = 3600000;

  @BeforeEach
  void setUp() {
    jwtService = new JwtService(SECRET, EXPIRATION_MS);
  }

  @Test
  void generateTokenCreatesValidToken() {
    String email = "test@example.com";
    String role = "USER";

    String token = jwtService.generateToken(email, role);

    assertThat(token).isNotNull();
    assertThat(token).isNotEmpty();
  }

  @Test
  void extractEmailReturnsCorrectEmail() {
    String email = "test@example.com";
    String role = "USER";

    String token = jwtService.generateToken(email, role);
    String extractedEmail = jwtService.extractEmail(token);

    assertThat(extractedEmail).isEqualTo(email);
  }

  @Test
  void extractRoleReturnsCorrectRole() {
    String email = "test@example.com";
    String role = "ADMIN";

    String token = jwtService.generateToken(email, role);
    String extractedRole = jwtService.extractRole(token);

    assertThat(extractedRole).isEqualTo(role);
  }

  @Test
  void isTokenValidReturnsTrueForValidToken() {
    String email = "test@example.com";
    String role = "USER";

    String token = jwtService.generateToken(email, role);
    boolean isValid = jwtService.isTokenValid(token, email);

    assertThat(isValid).isTrue();
  }

  @Test
  void isTokenValidReturnsFalseForInvalidEmail() {
    String email = "test@example.com";
    String role = "USER";

    String token = jwtService.generateToken(email, role);
    boolean isValid = jwtService.isTokenValid(token, "wrong@example.com");

    assertThat(isValid).isFalse();
  }

  @Test
  void isTokenValidReturnsFalseForExpiredToken() {
    JwtService shortExpiryJwtService = new JwtService(SECRET, -1000);
    String email = "test@example.com";
    String role = "USER";

    String token = shortExpiryJwtService.generateToken(email, role);
    boolean isValid = shortExpiryJwtService.isTokenValid(token, email);

    assertThat(isValid).isFalse();
  }
}