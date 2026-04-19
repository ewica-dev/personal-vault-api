package com.ewicadev.personalvaultapi.service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ewicadev.personalvaultapi.dto.auth.LoginRequest;
import com.ewicadev.personalvaultapi.dto.auth.LoginResponse;
import com.ewicadev.personalvaultapi.dto.auth.ResendVerificationResponse;
import com.ewicadev.personalvaultapi.dto.auth.SignupRequest;
import com.ewicadev.personalvaultapi.dto.auth.SignupResponse;
import com.ewicadev.personalvaultapi.dto.auth.VerifyEmailRequest;
import com.ewicadev.personalvaultapi.dto.auth.VerifyEmailResponse;
import com.ewicadev.personalvaultapi.entity.EmailJob;
import com.ewicadev.personalvaultapi.entity.EmailJobStatus;
import com.ewicadev.personalvaultapi.entity.EmailVerificationToken;
import com.ewicadev.personalvaultapi.entity.Role;
import com.ewicadev.personalvaultapi.entity.User;
import com.ewicadev.personalvaultapi.entity.EmailJobRepository;
import com.ewicadev.personalvaultapi.exception.DuplicateResourceException;
import com.ewicadev.personalvaultapi.exception.EmailNotVerifiedException;
import com.ewicadev.personalvaultapi.exception.InvalidCredentialsException;
import com.ewicadev.personalvaultapi.repository.EmailVerificationTokenRepository;
import com.ewicadev.personalvaultapi.repository.UserRepository;
import com.ewicadev.personalvaultapi.util.OtpGenerator;
import com.ewicadev.personalvaultapi.util.OtpHasher;

@Service
public class AuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthService.class);
  private static final int OTP_EXPIRY_MINUTES = 10;
  private static final int MAX_ATTEMPTS = 5;
  private static final int RESEND_COOLDOWN_SECONDS = 60;

  private final UserRepository userRepository;
  private final EmailVerificationTokenRepository tokenRepository;
  private final EmailJobRepository emailJobRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public AuthService(
      UserRepository userRepository,
      EmailVerificationTokenRepository tokenRepository,
      EmailJobRepository emailJobRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService
  ) {
    this.userRepository = userRepository;
    this.tokenRepository = tokenRepository;
    this.emailJobRepository = emailJobRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  @Transactional
  public SignupResponse register(SignupRequest request) {
    String normalizedEmail = normalizeEmail(request.getEmail());
    
    User user = new User();
    user.setEmail(normalizedEmail);
    user.setPassword(passwordEncoder.encode(request.getPassword()));
    user.setRole(Role.USER);
    user.setEmailVerified(false);

    User savedUser;
    try {
      savedUser = userRepository.save(user);
    } catch (DataIntegrityViolationException e) {
      User existing = userRepository.findByEmail(normalizedEmail)
          .orElseThrow(() -> new DuplicateResourceException("Email already in use"));
      if (existing.getEmailVerified()) {
        throw new DuplicateResourceException("Email already in use");
      }
      return handleResendFlow(existing);
    }

    String otp = OtpGenerator.generateOtp();
    String otpHash = OtpHasher.hash(otp);
    EmailVerificationToken token = createToken(savedUser, otpHash);
    tokenRepository.save(token);

    createEmailJob(normalizedEmail, otp, token.getExpiresAt().atZone(java.time.ZoneId.systemDefault()).toInstant());

    log.info("User registered, verification email queued: {}", maskEmail(normalizedEmail));
    return new SignupResponse(
        "User registered successfully. Please verify your email.",
        savedUser.getId(),
        savedUser.getEmail(),
        savedUser.getCreatedAt(),
        savedUser.getEmailVerified()
    );
  }

  @Transactional
  public ResendVerificationResponse resendVerification(String email) {
    String normalizedEmail = normalizeEmail(email);

    Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);
    if (userOpt.isEmpty()) {
      runDummyBcrypt();
      return new ResendVerificationResponse(
          "If the account exists and is not already verified, a verification email has been sent."
      );
    }

    User user = userOpt.get();
    if (user.getEmailVerified()) {
      runDummyBcrypt();
      log.info("Resend requested for already verified user: {}", maskEmail(normalizedEmail));
      return new ResendVerificationResponse(
          "If the account exists and is not already verified, a verification email has been sent."
      );
    }

    Optional<EmailVerificationToken> activeToken = tokenRepository.findActiveTokenByUserIdWithLock(user.getId());
    if (activeToken.isPresent()) {
      EmailVerificationToken token = activeToken.get();
      if (token.getCreatedAt().plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(LocalDateTime.now())) {
        runDummyBcrypt();
        return new ResendVerificationResponse(
            "If the account exists and is not already verified, a verification email has been sent."
        );
      }
    }

    String otp = OtpGenerator.generateOtp();
    String otpHash = OtpHasher.hash(otp);
    EmailVerificationToken newToken = createToken(user, otpHash);
    tokenRepository.save(newToken);

    createEmailJob(normalizedEmail, otp, newToken.getExpiresAt().atZone(java.time.ZoneId.systemDefault()).toInstant());

    log.info("Verification email queued for resend: {}", maskEmail(normalizedEmail));
    return new ResendVerificationResponse(
        "If the account exists and is not already verified, a verification email has been sent."
    );
  }

  @Transactional
  public VerifyEmailResponse verifyEmail(VerifyEmailRequest request) {
    String normalizedEmail = normalizeEmail(request.getEmail());

    Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);
    if (userOpt.isEmpty()) {
      runDummyBcrypt();
      return new VerifyEmailResponse(
          "Invalid or expired verification code. Please try again or request a new code.",
          false
      );
    }

    User user = userOpt.get();
    if (user.getEmailVerified()) {
      runDummyBcrypt();
      return new VerifyEmailResponse(
          "Invalid or expired verification code. Please try again or request a new code.",
          false
      );
    }

    Optional<EmailVerificationToken> activeToken = tokenRepository.findActiveTokenByUserIdWithLock(user.getId());
    if (activeToken.isEmpty()) {
      runDummyBcrypt();
      return new VerifyEmailResponse(
          "Invalid or expired verification code. Please try again or request a new code.",
          false
      );
    }

    EmailVerificationToken token = activeToken.get();

    if (token.getConsumedAt() != null || token.getExpiresAt().isBefore(LocalDateTime.now())) {
      log.warn("Token expired for user: {}", maskEmail(normalizedEmail));
      return new VerifyEmailResponse(
          "Invalid or expired verification code. Please try again or request a new code.",
          false
      );
    }

    if (token.getAttemptCount() >= MAX_ATTEMPTS) {
      token.setConsumedAt(LocalDateTime.now());
      tokenRepository.save(token);
      log.warn("Max attempts reached for user: {}", maskEmail(normalizedEmail));
      return new VerifyEmailResponse(
          "Invalid or expired verification code. Please try again or request a new code.",
          false
      );
    }

    if (!OtpHasher.verify(request.getOtp(), token.getOtpHash())) {
      token.setAttemptCount(token.getAttemptCount() + 1);
      tokenRepository.save(token);
      log.warn("Wrong OTP for user: {}, attempt: {}", maskEmail(normalizedEmail), token.getAttemptCount());
      return new VerifyEmailResponse(
          "Invalid or expired verification code. Please try again or request a new code.",
          false
      );
    }

    token.setConsumedAt(LocalDateTime.now());
    tokenRepository.save(token);

    user.setEmailVerified(true);
    userRepository.save(user);

    tokenRepository.consumeAllByUserIdExcept(user.getId(), token.getId());

    log.info("Email verified successfully: {}", maskEmail(normalizedEmail));
    return new VerifyEmailResponse("Email verified successfully.", true);
  }

  public LoginResponse login(LoginRequest request) {
    User user = userRepository.findByEmail(normalizeEmail(request.getEmail()))
        .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
      throw new InvalidCredentialsException("Invalid email or password");
    }

    if (!user.getEmailVerified()) {
      throw new EmailNotVerifiedException();
    }

    String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
    return LoginResponse.builder()
        .token(token)
        .tokenType("Bearer")
        .build();
  }

  private EmailVerificationToken createToken(User user, String otpHash) {
    EmailVerificationToken token = new EmailVerificationToken();
    token.setUser(user);
    token.setOtpHash(otpHash);
    token.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
    token.setAttemptCount(0);
    token.setCreatedAt(LocalDateTime.now());
    return token;
  }

  private void createEmailJob(String email, String otp, java.time.Instant expiresAt) {
    EmailJob job = new EmailJob();
    job.setEmail(email);
    job.setOtpCode(otp);
    job.setExpiresAt(expiresAt);
    job.setStatus(EmailJobStatus.PENDING);
    job.setAttemptCount(0);
    job.setCreatedAt(java.time.Instant.now());
    emailJobRepository.save(job);
    log.debug("Created EmailJob for {} in same transaction", email);
  }

  private SignupResponse handleResendFlow(User user) {
    String otp = OtpGenerator.generateOtp();
    String otpHash = OtpHasher.hash(otp);
    EmailVerificationToken token = createToken(user, otpHash);
    tokenRepository.save(token);

    createEmailJob(user.getEmail(), otp, token.getExpiresAt().atZone(java.time.ZoneId.systemDefault()).toInstant());

    log.info("Existing unverified user - verification email queued: {}", maskEmail(user.getEmail()));
    return new SignupResponse(
        "User registered successfully. Please verify your email.",
        user.getId(),
        user.getEmail(),
        user.getCreatedAt(),
        user.getEmailVerified()
    );
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  private String maskEmail(String email) {
    if (email == null || !email.contains("@")) {
      return "***";
    }
    int atIndex = email.indexOf("@");
    if (atIndex <= 2) {
      return "***" + email.substring(atIndex);
    }
    return email.substring(0, 2) + "***" + email.substring(atIndex);
  }

  private void runDummyBcrypt() {
    OtpHasher.hash("dummy");
  }
}