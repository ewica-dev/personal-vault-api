package com.ewicadev.personalvaultapi.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.ewicadev.personalvaultapi.entity.Role;
import com.ewicadev.personalvaultapi.entity.User;
import com.ewicadev.personalvaultapi.repository.UserRepository;

@Component
public class AdminBootstrap implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final String adminEmail;
  private final String adminPassword;
  private final boolean bootstrapEnabled;

  public AdminBootstrap(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      org.springframework.core.env.Environment environment) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.adminEmail = environment.getProperty("app.admin.email", "");
    this.adminPassword = environment.getProperty("app.admin.password", "");
    this.bootstrapEnabled =
        Boolean.parseBoolean(environment.getProperty("app.admin.bootstrap-enabled", "false"));
  }

  @Override
  public void run(String... args) {
    if (!bootstrapEnabled) {
      log.debug("Admin bootstrap is disabled");
      return;
    }

    if (adminEmail.isBlank() || adminPassword.isBlank()) {
      log.warn("Admin bootstrap is enabled but credentials are not configured");
      return;
    }

    if (userRepository.findByEmail(adminEmail).isPresent()) {
      log.debug("Admin user already exists, skipping bootstrap");
      return;
    }

    User admin = new User();
    admin.setEmail(adminEmail);
    admin.setPassword(passwordEncoder.encode(adminPassword));
    admin.setRole(Role.ADMIN);

    userRepository.save(admin);
    log.info("Admin user created: {}", adminEmail);
  }
}