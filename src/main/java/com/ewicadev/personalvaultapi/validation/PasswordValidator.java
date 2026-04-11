package com.ewicadev.personalvaultapi.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

  private static final int MIN_LENGTH = 8;
  private static final int MAX_LENGTH = 50;
  private static final String UPPERCASE = "[A-Z]";
  private static final String LOWERCASE = "[a-z]";
  private static final String DIGIT = "[0-9]";
  private static final String SPECIAL = "[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]";

  @Override
  public void initialize(ValidPassword constraintAnnotation) {
  }

  @Override
  public boolean isValid(String password, ConstraintValidatorContext context) {
    if (password == null || password.isBlank()) {
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate("Password is required").addConstraintViolation();
      return false;
    }

    if (password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate(
        "Password must be between " + MIN_LENGTH + " and " + MAX_LENGTH + " characters"
      ).addConstraintViolation();
      return false;
    }

    boolean hasUppercase = password.matches(".*" + UPPERCASE + ".*");
    boolean hasLowercase = password.matches(".*" + LOWERCASE + ".*");
    boolean hasDigit = password.matches(".*" + DIGIT + ".*");
    boolean hasSpecial = password.matches(".*" + SPECIAL + ".*");

    if (!hasUppercase || !hasLowercase || !hasDigit || !hasSpecial) {
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate(
        "Password must contain at least one uppercase letter, lowercase letter, digit, and special character"
      ).addConstraintViolation();
      return false;
    }

    return true;
  }
}
