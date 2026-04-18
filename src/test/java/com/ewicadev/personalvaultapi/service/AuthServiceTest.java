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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ewicadev.personalvaultapi.dto.auth.LoginRequest;
import com.ewicadev.personalvaultapi.dto.auth.LoginResponse;
import com.ewicadev.personalvaultapi.dto.auth.SignupRequest;
import com.ewicadev.personalvaultapi.dto.auth.SignupResponse;
import com.ewicadev.personalvaultapi.entity.User;
import com.ewicadev.personalvaultapi.exception.DuplicateResourceException;
import com.ewicadev.personalvaultapi.exception.InvalidCredentialsException;
import com.ewicadev.personalvaultapi.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private JwtService jwtService;

  @InjectMocks
  private AuthService authService;

  private SignupRequest signupRequest;
  private LoginRequest loginRequest;
  private User user;

  @BeforeEach
  void setUp() {
    signupRequest = new SignupRequest();
    signupRequest.setEmail("test@example.com");
    signupRequest.setPassword("password123");

    loginRequest = new LoginRequest();
    loginRequest.setEmail("test@example.com");
    loginRequest.setPassword("password123");

    user = new User();
    user.setId(1L);
    user.setEmail("test@example.com");
    user.setPassword("encodedPassword");
    user.setCreatedAt(LocalDateTime.now());
  }

  @Test
  void registerValidRequestReturnsSignupResponse() {
    when(userRepository.findByEmail(signupRequest.getEmail())).thenReturn(Optional.empty());
    when(passwordEncoder.encode(signupRequest.getPassword())).thenReturn("encodedPassword");
    when(userRepository.save(any(User.class))).thenReturn(user);

    SignupResponse result = authService.register(signupRequest);

    assertThat(result).isNotNull();
    assertThat(result.getMessage()).isEqualTo("User registered successfully");
    assertThat(result.getUser().getId()).isEqualTo(1L);
    assertThat(result.getUser().getEmail()).isEqualTo("test@example.com");
    verify(userRepository).findByEmail(signupRequest.getEmail());
    verify(userRepository).save(any(User.class));
  }

  @Test
  void registerDuplicateEmailThrowsException() {
    when(userRepository.findByEmail(signupRequest.getEmail())).thenReturn(Optional.of(user));

    assertThatThrownBy(() -> authService.register(signupRequest))
        .isInstanceOf(DuplicateResourceException.class)
        .hasMessageContaining("Email already in use");

    verify(userRepository).findByEmail(signupRequest.getEmail());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void loginValidCredentialsReturnsToken() {
    String jwtToken = "jwt-token-value";

    when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())).thenReturn(true);
    when(jwtService.generateToken(user.getEmail(), user.getRole().name())).thenReturn(jwtToken);

    LoginResponse result = authService.login(loginRequest);

    assertThat(result).isNotNull();
    assertThat(result.getToken()).isEqualTo(jwtToken);
    assertThat(result.getTokenType()).isEqualTo("Bearer");
    verify(userRepository).findByEmail(loginRequest.getEmail());
    verify(passwordEncoder).matches(loginRequest.getPassword(), user.getPassword());
    verify(jwtService).generateToken(user.getEmail(), user.getRole().name());
  }

  @Test
  void loginInvalidEmailThrowsException() {
    when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.login(loginRequest))
        .isInstanceOf(InvalidCredentialsException.class)
        .hasMessageContaining("Invalid email or password");

    verify(userRepository).findByEmail(loginRequest.getEmail());
    verify(passwordEncoder, never()).matches(anyString(), anyString());
    verify(jwtService, never()).generateToken(anyString(), anyString());
  }

  @Test
  void loginInvalidPasswordThrowsException() {
    when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())).thenReturn(false);

    assertThatThrownBy(() -> authService.login(loginRequest))
        .isInstanceOf(InvalidCredentialsException.class)
        .hasMessageContaining("Invalid email or password");

    verify(userRepository).findByEmail(loginRequest.getEmail());
    verify(passwordEncoder).matches(loginRequest.getPassword(), user.getPassword());
    verify(jwtService, never()).generateToken(anyString(), anyString());
  }
}