package com.ewicadev.personalvaultapi.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OtpGeneratorTest {

  @Test
  @DisplayName("generates exactly 6 digits")
  void generatesExactlySixDigits() {
    String otp = OtpGenerator.generateOtp();
    assertEquals(6, otp.length());
  }

  @Test
  @DisplayName("contains only numeric digits")
  void containsOnlyNumericDigits() {
    String otp = OtpGenerator.generateOtp();
    assertTrue(otp.matches("\\d{6}"));
  }

  @Test
  @DisplayName("allows leading zeros")
  void allowsLeadingZeros() {
    boolean foundLeadingZero = false;
    for (int i = 0; i < 100; i++) {
      String otp = OtpGenerator.generateOtp();
      if (otp.startsWith("0")) {
        foundLeadingZero = true;
        break;
      }
    }
    assertTrue(foundLeadingZero, "Should generate OTPs with leading zeros");
  }
}