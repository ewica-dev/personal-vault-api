package com.ewicadev.personalvaultapi.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = PasswordValidator.class)
@Documented
public @interface ValidPassword {
  String message() default "Password must contain at least 8 characters with uppercase, lowercase, digit, and special character";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};
}
