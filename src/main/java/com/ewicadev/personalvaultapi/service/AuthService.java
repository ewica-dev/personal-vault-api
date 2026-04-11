package com.ewicadev.personalvaultapi.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ewicadev.personalvaultapi.dto.LoginRequest;
import com.ewicadev.personalvaultapi.dto.SignupRequest;
import com.ewicadev.personalvaultapi.entity.User;
import com.ewicadev.personalvaultapi.repository.UserRepository;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  public User register(SignupRequest request) {
    if (userRepository.findByEmail(request.getEmail()).isPresent()) {
      throw new RuntimeException("Email already in use");
    }

    User user = new User();
    user.setEmail(request.getEmail());
    // Hash the password before saving
    user.setPassword(passwordEncoder.encode(request.getPassword()));

    return userRepository.save(user);
  }

  public String login(LoginRequest request) {
    User user = userRepository.findByEmail(request.getEmail())
      .orElseThrow(() -> new RuntimeException("Invalid email or password"));

    // Verify the raw password against the hashed one in the DB
    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
      throw new RuntimeException("Invalid email or password");
    }

    return "Login successful! (Token logic comes next)";
  }
}