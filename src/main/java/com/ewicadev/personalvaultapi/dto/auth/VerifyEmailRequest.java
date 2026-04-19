package com.ewicadev.personalvaultapi.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VerifyEmailRequest {

  @NotBlank(message = "Email is required")
  @Email(message = "Invalid email format")
  private String email;

  @NotBlank(message = "OTP is required")
  @Size(min = 6, max = 6, message = "OTP must be 6 digits")
  @Pattern(regexp = "^\\d{6}$", message = "OTP must be exactly 6 numeric digits")
  private String otp;
}