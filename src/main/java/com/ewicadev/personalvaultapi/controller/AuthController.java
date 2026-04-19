package com.ewicadev.personalvaultapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ewicadev.personalvaultapi.dto.auth.LoginRequest;
import com.ewicadev.personalvaultapi.dto.auth.LoginResponse;
import com.ewicadev.personalvaultapi.dto.auth.ResendVerificationRequest;
import com.ewicadev.personalvaultapi.dto.auth.ResendVerificationResponse;
import com.ewicadev.personalvaultapi.dto.auth.SignupRequest;
import com.ewicadev.personalvaultapi.dto.auth.SignupResponse;
import com.ewicadev.personalvaultapi.dto.auth.VerifyEmailRequest;
import com.ewicadev.personalvaultapi.dto.auth.VerifyEmailResponse;
import com.ewicadev.personalvaultapi.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/signup")
  public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
    return ResponseEntity.ok(authService.register(request));
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(authService.login(request));
  }

  @PostMapping("/verify-email")
  public ResponseEntity<VerifyEmailResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
    return ResponseEntity.ok(authService.verifyEmail(request));
  }

  @PostMapping("/resend-verification")
  public ResponseEntity<ResendVerificationResponse> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
    return ResponseEntity.ok(authService.resendVerification(request.getEmail()));
  }
}