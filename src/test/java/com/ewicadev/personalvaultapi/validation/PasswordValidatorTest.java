package com.ewicadev.personalvaultapi.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PasswordValidatorTest {

  private PasswordValidator validator;

  @BeforeEach
  void setUp() {
    validator = new PasswordValidator();
  }

  @Test
  @DisplayName("valid password passes validation")
  void validPasswordPasses() {
    boolean result = validator.isValid("Password123!", null);
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("password with all requirements passes")
  void passwordWithAllRequirementsPasses() {
    boolean result = validator.isValid("MyP@ssw0rd!", null);
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("password at minimum length passes")
  void passwordAtMinLengthPasses() {
    boolean result = validator.isValid("Pass123!", null);
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("complex password passes")
  void complexPasswordPasses() {
    boolean result = validator.isValid("Str0ng!P@ssword", null);
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("password with different special characters passes")
  void passwordWithDifferentSpecialCharsPasses() {
    boolean result = validator.isValid("Test#123Aa", null);
    assertThat(result).isTrue();
  }
}