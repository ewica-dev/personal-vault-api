package com.ewicadev.personalvaultapi.util;

import java.security.SecureRandom;

public final class OtpGenerator {

  private static final int OTP_LENGTH = 6;
  private static final SecureRandom RANDOM = new SecureRandom();

  private OtpGenerator() {
  }

  public static String generateOtp() {
    int otp = RANDOM.nextInt((int) Math.pow(10, OTP_LENGTH));
    return String.format("%06d", otp);
  }
}