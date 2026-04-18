package com.ewicadev.personalvaultapi.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ewicadev.personalvaultapi.dto.auth.LoginRequest;
import com.ewicadev.personalvaultapi.dto.auth.LoginResponse;
import com.ewicadev.personalvaultapi.dto.auth.SignupRequest;
import com.ewicadev.personalvaultapi.dto.auth.SignupResponse;
import com.ewicadev.personalvaultapi.entity.Role;
import com.ewicadev.personalvaultapi.entity.User;
import com.ewicadev.personalvaultapi.exception.DuplicateResourceException;
import com.ewicadev.personalvaultapi.exception.InvalidCredentialsException;
import com.ewicadev.personalvaultapi.repository.UserRepository;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public AuthService(
    UserRepository userRepository, 
    PasswordEncoder passwordEncoder,
    JwtService jwtService
  ) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  public SignupResponse register(SignupRequest request) {
    if (userRepository.findByEmail(request.getEmail()).isPresent()) {
      throw new DuplicateResourceException("Email already in use");
    }

    User user = new User();
    user.setEmail(request.getEmail());
    user.setPassword(passwordEncoder.encode(request.getPassword()));
    user.setRole(Role.USER);

    User savedUser = userRepository.save(user);
    return new SignupResponse(
      "User registered successfully",
      savedUser.getId(),
      savedUser.getEmail(),
      savedUser.getCreatedAt()
    );
  }

  public LoginResponse login(LoginRequest request) {
    User user = userRepository.findByEmail(request.getEmail())
      .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
      throw new InvalidCredentialsException("Invalid email or password");
    }

    String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
    return LoginResponse.builder()
        .token(token)
        .tokenType("Bearer")
        .build();
  }
}