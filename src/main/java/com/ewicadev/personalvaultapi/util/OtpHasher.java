package com.ewicadev.personalvaultapi.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class OtpHasher {

  private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(10);

  private OtpHasher() {
  }

  public static String hash(String otp) {
    return ENCODER.encode(otp);
  }

  public static boolean verify(String otp, String hash) {
    return ENCODER.matches(otp, hash);
  }
}