package com.ewicadev.personalvaultapi.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RateLimitServiceTest {

  private final RateLimitService rateLimitService = new RateLimitService();

  @Test
  @DisplayName("signup IP limit allows requests within limit")
  void signupIpAllowsWithinLimit() {
    for (int i = 0; i < 5; i++) {
      assertTrue(rateLimitService.checkSignupIpLimit("192.168.1.1"));
    }
  }

  @Test
  @DisplayName("signup IP limit blocks when exceeded")
  void signupIpBlocksWhenExceeded() {
    for (int i = 0; i < 5; i++) {
      rateLimitService.checkSignupIpLimit("192.168.1.2");
    }
    assertFalse(rateLimitService.checkSignupIpLimit("192.168.1.2"), "Should be blocked after 5 requests");
  }

  @Test
  @DisplayName("signup email limit allows requests within limit")
  void signupEmailAllowsWithinLimit() {
    for (int i = 0; i < 3; i++) {
      assertTrue(rateLimitService.checkSignupEmailLimit("test@example.com"));
    }
  }

  @Test
  @DisplayName("signup email limit blocks when exceeded")
  void signupEmailBlocksWhenExceeded() {
    for (int i = 0; i < 3; i++) {
      rateLimitService.checkSignupEmailLimit("test2@example.com");
    }
    assertFalse(rateLimitService.checkSignupEmailLimit("test2@example.com"), "Should be blocked after 3 requests");
  }

  @Test
  @DisplayName("resend IP limit enforces correctly")
  void resendIpEnforcesCorrectly() {
    for (int i = 0; i < 5; i++) {
      assertTrue(rateLimitService.checkResendIpLimit("10.0.0.1"));
    }
    assertFalse(rateLimitService.checkResendIpLimit("10.0.0.1"));
  }

  @Test
  @DisplayName("verify IP limit enforces correctly")
  void verifyIpEnforcesCorrectly() {
    for (int i = 0; i < 10; i++) {
      assertTrue(rateLimitService.checkVerifyIpLimit("172.16.0.1"));
    }
    assertFalse(rateLimitService.checkVerifyIpLimit("172.16.0.1"));
  }

  @Test
  @DisplayName("different IPs tracked separately")
  void differentIpsTrackedSeparately() {
    for (int i = 0; i < 5; i++) {
      assertTrue(rateLimitService.checkSignupIpLimit("10.1.1.1"));
    }
    assertFalse(rateLimitService.checkSignupIpLimit("10.1.1.1"));

    for (int i = 0; i < 5; i++) {
      assertTrue(rateLimitService.checkSignupIpLimit("10.1.1.2"));
    }
    assertFalse(rateLimitService.checkSignupIpLimit("10.1.1.2"));
  }
}