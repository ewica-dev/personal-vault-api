package com.ewicadev.personalvaultapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

  private static final Logger log = LoggerFactory.getLogger(EmailService.class);

  private final JavaMailSender mailSender;
  private final String fromAddress;

  public EmailService(
      JavaMailSender mailSender,
      @Value("${app.mail.from}") String fromAddress) {
    this.mailSender = mailSender;
    this.fromAddress = fromAddress;
  }

  @Async
  public void sendVerificationEmail(String email, String otp) {
    try {
      SimpleMailMessage message = new SimpleMailMessage();
      message.setFrom(fromAddress);
      message.setTo(email);
      message.setSubject("Verify your email - Personal Vault");
      message.setText(
          "Your verification code is: " + otp + "\n\n"
              + "This code expires in 10 minutes.\n"
              + "If you did not request this, please ignore this email.");
      mailSender.send(message);
      log.info("Verification email sent to {}", maskEmail(email));
    } catch (Exception e) {
      log.error("Failed to send verification email to {}: {}", maskEmail(email), e.getMessage());
    }
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
}