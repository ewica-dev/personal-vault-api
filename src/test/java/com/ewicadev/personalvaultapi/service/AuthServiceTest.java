package com.ewicadev.personalvaultapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ewicadev.personalvaultapi.dto.auth.LoginRequest;
import com.ewicadev.personalvaultapi.dto.auth.ResendVerificationResponse;
import com.ewicadev.personalvaultapi.dto.auth.SignupRequest;
import com.ewicadev.personalvaultapi.dto.auth.VerifyEmailRequest;
import com.ewicadev.personalvaultapi.dto.auth.VerifyEmailResponse;
import com.ewicadev.personalvaultapi.entity.EmailVerificationToken;
import com.ewicadev.personalvaultapi.entity.User;
import com.ewicadev.personalvaultapi.exception.DuplicateResourceException;
import com.ewicadev.personalvaultapi.exception.EmailNotVerifiedException;
import com.ewicadev.personalvaultapi.exception.InvalidCredentialsException;
import com.ewicadev.personalvaultapi.repository.EmailVerificationTokenRepository;
import com.ewicadev.personalvaultapi.repository.UserRepository;
import com.ewicadev.personalvaultapi.util.OtpHasher;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private EmailVerificationTokenRepository tokenRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private JwtService jwtService;

  @Mock
  private EmailService emailService;

  @InjectMocks
  private AuthService authService;

  private SignupRequest signupRequest;
  private LoginRequest loginRequest;
  private User user;

  @BeforeEach
  void setUp() {
    signupRequest = new SignupRequest();
    signupRequest.setEmail("test@example.com");
    signupRequest.setPassword("Password123!");

    loginRequest = new LoginRequest();
    loginRequest.setEmail("test@example.com");
    loginRequest.setPassword("Password123!");

    user = new User();
    user.setId(1L);
    user.setEmail("test@example.com");
    user.setPassword("encodedPassword");
    user.setCreatedAt(LocalDateTime.now());
    user.setEmailVerified(true);
  }

  @Test
  @DisplayName("register creates unverified user and sends OTP")
  void registerCreatesUnverifiedUserAndSendsOtp() {
    User unverifiedUser = new User();
    unverifiedUser.setId(1L);
    unverifiedUser.setEmail("test@example.com");
    unverifiedUser.setPassword("encodedPassword");
    unverifiedUser.setCreatedAt(LocalDateTime.now());
    unverifiedUser.setEmailVerified(false);

    when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
    when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
    when(userRepository.save(any(User.class))).thenReturn(unverifiedUser);

    var result = authService.register(signupRequest);

    assertThat(result).isNotNull();
    assertThat(result.getMessage()).contains("verify your email");
    assertThat(result.getUser().isEmailVerified()).isFalse();
    verify(tokenRepository).save(any(EmailVerificationToken.class));
  }

  @Test
  @DisplayName("register throws DuplicateResourceException for existing verified user")
  void registerThrowsForExistingVerifiedUser() {
    User verifiedUser = new User();
    verifiedUser.setId(1L);
    verifiedUser.setEmail("test@example.com");
    verifiedUser.setPassword("encodedPassword");
    verifiedUser.setCreatedAt(LocalDateTime.now());
    verifiedUser.setEmailVerified(true);

    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
    when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
    when(userRepository.save(any(User.class)))
        .thenThrow(new DataIntegrityViolationException("Duplicate entry"));
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(verifiedUser));

    assertThatThrownBy(() -> authService.register(signupRequest))
        .isInstanceOf(DuplicateResourceException.class);
  }

  @Test
  @DisplayName("verifyEmail returns success for correct OTP")
  void verifyEmailReturnsSuccessForCorrectOtp() {
    user.setEmailVerified(false);
    String validOtp = "123456";
    String otpHash = OtpHasher.hash(validOtp);

    EmailVerificationToken token = new EmailVerificationToken();
    token.setId(1L);
    token.setUser(user);
    token.setOtpHash(otpHash);
    token.setExpiresAt(LocalDateTime.now().plusMinutes(10));
    token.setAttemptCount(0);
    token.setCreatedAt(LocalDateTime.now());

    when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
    when(tokenRepository.findActiveTokenByUserIdWithLock(user.getId())).thenReturn(Optional.of(token));
    when(tokenRepository.save(any())).thenReturn(token);
    when(userRepository.save(any())).thenReturn(user);
    when(tokenRepository.consumeAllByUserIdExcept(any(), any())).thenReturn(0);

    VerifyEmailRequest request = new VerifyEmailRequest();
    request.setEmail("test@example.com");
    request.setOtp(validOtp);

    VerifyEmailResponse result = authService.verifyEmail(request);

    assertThat(result.isVerified()).isTrue();
    assertThat(result.getMessage()).contains("successfully");
  }

  @Test
  @DisplayName("verifyEmail returns generic error for wrong OTP and increments attempt")
  void verifyEmailReturnsErrorForWrongOtpAndIncrementsAttempt() {
    user.setEmailVerified(false);
    String wrongOtp = "654321";
    String correctOtp = "123456";
    String otpHash = OtpHasher.hash(correctOtp);

    EmailVerificationToken token = new EmailVerificationToken();
    token.setId(1L);
    token.setUser(user);
    token.setOtpHash(otpHash);
    token.setExpiresAt(LocalDateTime.now().plusMinutes(10));
    token.setAttemptCount(0);
    token.setCreatedAt(LocalDateTime.now());

    when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
    when(tokenRepository.findActiveTokenByUserIdWithLock(user.getId())).thenReturn(Optional.of(token));
    when(tokenRepository.save(any())).thenReturn(token);

    VerifyEmailRequest request = new VerifyEmailRequest();
    request.setEmail("test@example.com");
    request.setOtp(wrongOtp);

    VerifyEmailResponse result = authService.verifyEmail(request);

    assertThat(result.isVerified()).isFalse();
    assertThat(result.getMessage()).contains("Invalid or expired");
    verify(tokenRepository).save(token);
  }

  @Test
  @DisplayName("verifyEmail returns error for expired token")
  void verifyEmailReturnsErrorForExpiredToken() {
    user.setEmailVerified(false);
    String otpHash = OtpHasher.hash("123456");

    EmailVerificationToken token = new EmailVerificationToken();
    token.setId(1L);
    token.setUser(user);
    token.setOtpHash(otpHash);
    token.setExpiresAt(LocalDateTime.now().minusMinutes(1));
    token.setAttemptCount(0);
    token.setCreatedAt(LocalDateTime.now().minusMinutes(15));

    when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
    when(tokenRepository.findActiveTokenByUserIdWithLock(user.getId())).thenReturn(Optional.of(token));

    VerifyEmailRequest request = new VerifyEmailRequest();
    request.setEmail("test@example.com");
    request.setOtp("123456");

    VerifyEmailResponse result = authService.verifyEmail(request);

    assertThat(result.isVerified()).isFalse();
    assertThat(result.getMessage()).contains("Invalid or expired");
  }

  @Test
  @DisplayName("verifyEmail returns error when max attempts reached")
  void verifyEmailReturnsErrorWhenMaxAttemptsReached() {
    user.setEmailVerified(false);
    String otpHash = OtpHasher.hash("123456");

    EmailVerificationToken token = new EmailVerificationToken();
    token.setId(1L);
    token.setUser(user);
    token.setOtpHash(otpHash);
    token.setExpiresAt(LocalDateTime.now().plusMinutes(10));
    token.setAttemptCount(5);
    token.setCreatedAt(LocalDateTime.now());

    when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
    when(tokenRepository.findActiveTokenByUserIdWithLock(user.getId())).thenReturn(Optional.of(token));
    when(tokenRepository.save(any())).thenReturn(token);

    VerifyEmailRequest request = new VerifyEmailRequest();
    request.setEmail("test@example.com");
    request.setOtp("123456");

    VerifyEmailResponse result = authService.verifyEmail(request);

    assertThat(result.isVerified()).isFalse();
    assertThat(token.getConsumedAt()).isNotNull();
  }

  @Test
  @DisplayName("resendVerification returns generic message for non-existent email")
  void resendVerificationReturnsGenericMessageForNonExistentEmail() {
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

    var result = authService.resendVerification("nonexistent@example.com");

    assertThat(result.getMessage()).contains("If the account exists");
  }

  @Test
  @DisplayName("resendVerification returns generic message for already verified user")
  void resendVerificationReturnsGenericMessageForAlreadyVerifiedUser() {
    user.setEmailVerified(true);
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

    var result = authService.resendVerification("test@example.com");

    assertThat(result.getMessage()).contains("If the account exists");
  }

  @Test
  @DisplayName("login throws EmailNotVerifiedException for unverified user")
  void loginThrowsEmailNotVerifiedExceptionForUnverifiedUser() {
    user.setEmailVerified(false);
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

    assertThatThrownBy(() -> authService.login(loginRequest))
        .isInstanceOf(EmailNotVerifiedException.class);

    verify(jwtService, never()).generateToken(anyString(), anyString());
  }

  @Test
  @DisplayName("login returns token for verified user")
  void loginReturnsTokenForVerifiedUser() {
    String jwtToken = "jwt-token";
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
    when(jwtService.generateToken(anyString(), anyString())).thenReturn(jwtToken);

    var result = authService.login(loginRequest);

    assertThat(result.getToken()).isEqualTo(jwtToken);
  }
}