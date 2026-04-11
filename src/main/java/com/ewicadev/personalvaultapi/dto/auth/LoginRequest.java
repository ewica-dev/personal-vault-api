package com.ewicadev.personalvaultapi.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {
  @NotBlank(message = "Email is required")
  @Email(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", 
        message = "Invalid email format")
  @Size(max = 255, message = "Email must be under 255 characters")
  private String email;

  @NotBlank(message = "Password is required")
  @Size(max = 100, message = "Password is too long")
  private String password;
}
