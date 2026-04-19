package com.ewicadev.personalvaultapi.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OtpHasherTest {

  @Test
  @DisplayName("hash produces different output each time due to salt")
  void hashProducesDifferentOutputEachTime() {
    String otp = "123456";
    String hash1 = OtpHasher.hash(otp);
    String hash2 = OtpHasher.hash(otp);
    assertNotEquals(hash1, hash2, "Hashes should be different due to random salt");
  }

  @Test
  @DisplayName("verify returns true for correct OTP")
  void verifyReturnsTrueForCorrectOtp() {
    String otp = "123456";
    String hash = OtpHasher.hash(otp);
    assertTrue(OtpHasher.verify(otp, hash));
  }

  @Test
  @DisplayName("verify returns false for wrong OTP")
  void verifyReturnsFalseForWrongOtp() {
    String otp = "123456";
    String wrongOtp = "654321";
    String hash = OtpHasher.hash(otp);
    assertFalse(OtpHasher.verify(wrongOtp, hash));
  }
}