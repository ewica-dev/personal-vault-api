package com.ewicadev.personalvaultapi.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.ewicadev.personalvaultapi.dto.auth.LoginRequest;
import com.ewicadev.personalvaultapi.dto.auth.LoginResponse;
import com.ewicadev.personalvaultapi.dto.auth.ResendVerificationRequest;
import com.ewicadev.personalvaultapi.dto.auth.ResendVerificationResponse;
import com.ewicadev.personalvaultapi.dto.auth.SignupRequest;
import com.ewicadev.personalvaultapi.dto.auth.SignupResponse;
import com.ewicadev.personalvaultapi.dto.auth.VerifyEmailRequest;
import com.ewicadev.personalvaultapi.dto.auth.VerifyEmailResponse;
import com.ewicadev.personalvaultapi.exception.EmailNotVerifiedException;
import com.ewicadev.personalvaultapi.exception.InvalidCredentialsException;
import com.ewicadev.personalvaultapi.service.AuthService;

import java.time.LocalDateTime;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  @Mock
  private AuthService authService;

  @InjectMocks
  private AuthController authController;

  private SignupRequest signupRequest;
  private LoginRequest loginRequest;
  private VerifyEmailRequest verifyEmailRequest;
  private ResendVerificationRequest resendRequest;

  @BeforeEach
  void setUp() {
    signupRequest = new SignupRequest();
    signupRequest.setEmail("test@example.com");
    signupRequest.setPassword("Password123!");

    loginRequest = new LoginRequest();
    loginRequest.setEmail("test@example.com");
    loginRequest.setPassword("Password123!");

    verifyEmailRequest = new VerifyEmailRequest();
    verifyEmailRequest.setEmail("test@example.com");
    verifyEmailRequest.setOtp("123456");

    resendRequest = new ResendVerificationRequest();
    resendRequest.setEmail("test@example.com");
  }

  @Test
  @DisplayName("signup returns 200 with valid request")
  void signupReturns200WithValidRequest() {
    SignupResponse response = new SignupResponse(
        "User registered successfully. Please verify your email.",
        1L,
        "test@example.com",
        LocalDateTime.now(),
        false
    );
    when(authService.register(any())).thenReturn(response);

    ResponseEntity<SignupResponse> result = authController.signup(signupRequest);

    assertThat(result.getStatusCode().value()).isEqualTo(200);
    assertThat(result.getBody()).isNotNull();
    assertThat(result.getBody().getMessage()).contains("verify your email");
  }

  @Test
  @DisplayName("verify-email returns 200 with valid request")
  void verifyEmailReturns200WithValidRequest() {
    VerifyEmailResponse response = new VerifyEmailResponse("Email verified successfully.", true);
    when(authService.verifyEmail(any())).thenReturn(response);

    ResponseEntity<VerifyEmailResponse> result = authController.verifyEmail(verifyEmailRequest);

    assertThat(result.getStatusCode().value()).isEqualTo(200);
    assertThat(result.getBody()).isNotNull();
    assertThat(result.getBody().isVerified()).isTrue();
  }

  @Test
  @DisplayName("verify-email returns 200 with invalid OTP (400 behavior in response)")
  void verifyEmailReturns400BehaviorWithInvalidOtp() {
    VerifyEmailResponse response = new VerifyEmailResponse(
        "Invalid or expired verification code. Please try again or request a new code.",
        false
    );
    when(authService.verifyEmail(any())).thenReturn(response);

    ResponseEntity<VerifyEmailResponse> result = authController.verifyEmail(verifyEmailRequest);

    assertThat(result.getStatusCode().value()).isEqualTo(200);
    assertThat(result.getBody()).isNotNull();
    assertThat(result.getBody().isVerified()).isFalse();
  }

  @Test
  @DisplayName("resend-verification returns 200 with valid request")
  void resendVerificationReturns200WithValidRequest() {
    ResendVerificationResponse response = new ResendVerificationResponse(
        "If the account exists and is not already verified, a verification email has been sent."
    );
    when(authService.resendVerification(any())).thenReturn(response);

    ResponseEntity<ResendVerificationResponse> result = authController.resendVerification(resendRequest);

    assertThat(result.getStatusCode().value()).isEqualTo(200);
    assertThat(result.getBody()).isNotNull();
    assertThat(result.getBody().getMessage()).contains("If the account exists");
    verify(authService).resendVerification("test@example.com");
  }

  @Test
  @DisplayName("POST /login returns 200 with valid credentials")
  void loginReturns200() {
    LoginResponse response = LoginResponse.builder()
        .token("jwt-token")
        .tokenType("Bearer")
        .build();
    when(authService.login(any())).thenReturn(response);

    ResponseEntity<LoginResponse> result = authController.login(loginRequest);

    assertThat(result.getStatusCode().value()).isEqualTo(200);
    assertThat(result.getBody()).isNotNull();
    assertThat(result.getBody().getToken()).isEqualTo("jwt-token");
  }

  @Test
  @DisplayName("POST /login throws InvalidCredentialsException for wrong password")
  void loginThrowsInvalidCredentials() {
    when(authService.login(any()))
        .thenThrow(new InvalidCredentialsException("Invalid email or password"));

    assertThatThrownBy(() -> authController.login(loginRequest))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  @DisplayName("POST /login throws EmailNotVerifiedException for unverified user")
  void loginThrowsEmailNotVerified() {
    when(authService.login(any()))
        .thenThrow(new EmailNotVerifiedException());

    assertThatThrownBy(() -> authController.login(loginRequest))
        .isInstanceOf(EmailNotVerifiedException.class);
  }
}