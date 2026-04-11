package com.ewicadev.personalvaultapi.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.ewicadev.personalvaultapi.dto.common.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationExceptions(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach((error) -> {
      String fieldName = ((FieldError) error).getField();
      String errorMessage = error.getDefaultMessage();
      errors.put(fieldName, errorMessage);
    });

    ErrorResponse errorRes = ErrorResponse.builder()
      .timestamp(LocalDateTime.now())
      .status(HttpStatus.BAD_REQUEST.value())
      .error("Validation Failed")
      .errorCode("VALIDATION_ERROR")
      .message("One or more fields failed validation")
      .path(request.getRequestURI())
      .validationErrors(errors)
      .build();

    return new ResponseEntity<>(errorRes, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(
      ConstraintViolationException ex, HttpServletRequest request) {
    
    Map<String, String> errors = new HashMap<>();
    ex.getConstraintViolations().forEach(violation -> {
      String field = violation.getPropertyPath().toString();
      errors.put(field, violation.getMessage());
    });

    ErrorResponse errorRes = ErrorResponse.builder()
      .timestamp(LocalDateTime.now())
      .status(HttpStatus.BAD_REQUEST.value())
      .error("Constraint Violation")
      .errorCode("CONSTRAINT_VIOLATION")
      .message("One or more constraints were violated")
      .path(request.getRequestURI())
      .validationErrors(errors)
      .build();

    return new ResponseEntity<>(errorRes, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
      DataIntegrityViolationException ex, HttpServletRequest request) {
    
    String message = "A data integrity constraint was violated";
    String errorCode = "DATA_INTEGRITY_ERROR";
    
    String causeMessage = ex.getMostSpecificCause().getMessage();
    if (causeMessage != null) {
      if (causeMessage.contains("unique") || causeMessage.contains("duplicate")) {
        message = "A record with this value already exists";
        errorCode = "DUPLICATE_ENTRY";
      } else if (causeMessage.contains("foreign key")) {
        message = "Referenced record does not exist";
        errorCode = "FOREIGN_KEY_VIOLATION";
      } else if (causeMessage.contains("not null")) {
        message = "Required field is missing";
        errorCode = "NOT_NULL_VIOLATION";
      }
    }

    ErrorResponse errorRes = ErrorResponse.builder()
      .timestamp(LocalDateTime.now())
      .status(HttpStatus.CONFLICT.value())
      .error("Data Conflict")
      .errorCode(errorCode)
      .message(message)
      .path(request.getRequestURI())
      .build();

    return new ResponseEntity<>(errorRes, HttpStatus.CONFLICT);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex, HttpServletRequest request) {
    
    ErrorResponse errorRes = ErrorResponse.builder()
      .timestamp(LocalDateTime.now())
      .status(HttpStatus.BAD_REQUEST.value())
      .error("Malformed Request")
      .errorCode("MALFORMED_JSON")
      .message("Invalid JSON format in request body")
      .path(request.getRequestURI())
      .build();

    return new ResponseEntity<>(errorRes, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(
      MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
    
    String paramName = ex.getName();
    String expectedType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
    
    ErrorResponse errorRes = ErrorResponse.builder()
      .timestamp(LocalDateTime.now())
      .status(HttpStatus.BAD_REQUEST.value())
      .error("Invalid Parameter Type")
      .errorCode("INVALID_PARAMETER_TYPE")
      .message("Parameter '" + paramName + "' must be of type " + expectedType)
      .path(request.getRequestURI())
      .build();

    return new ResponseEntity<>(errorRes, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDenied(
      AccessDeniedException ex, HttpServletRequest request) {
    
    ErrorResponse errorRes = ErrorResponse.builder()
      .timestamp(LocalDateTime.now())
      .status(HttpStatus.FORBIDDEN.value())
      .error("Access Denied")
      .errorCode("ACCESS_DENIED")
      .message("You do not have permission to perform this action")
      .path(request.getRequestURI())
      .build();

    return new ResponseEntity<>(errorRes, HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ErrorResponse> handleBadCredentials(
      BadCredentialsException ex, HttpServletRequest request) {
    
    ErrorResponse errorRes = ErrorResponse.builder()
      .timestamp(LocalDateTime.now())
      .status(HttpStatus.UNAUTHORIZED.value())
      .error("Invalid Credentials")
      .errorCode("INVALID_CREDENTIALS")
      .message("Email or password is incorrect")
      .path(request.getRequestURI())
      .build();

    return new ResponseEntity<>(errorRes, HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorResponse> handleAuthenticationException(
      AuthenticationException ex, HttpServletRequest request) {
    
    ErrorResponse errorRes = ErrorResponse.builder()
      .timestamp(LocalDateTime.now())
      .status(HttpStatus.UNAUTHORIZED.value())
      .error("Unauthorized")
      .errorCode("UNAUTHORIZED")
      .message("User is not authorized to perform this action")
      .path(request.getRequestURI())
      .build();

    return new ResponseEntity<>(errorRes, HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(
      IllegalArgumentException ex, HttpServletRequest request) {
    
    ErrorResponse errorRes = ErrorResponse.builder()
      .timestamp(LocalDateTime.now())
      .status(HttpStatus.BAD_REQUEST.value())
      .error("Invalid Argument")
      .errorCode("INVALID_ARGUMENT")
      .message(ex.getMessage())
      .path(request.getRequestURI())
      .build();

    return new ResponseEntity<>(errorRes, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleResourceNotFound(
      ResourceNotFoundException ex, HttpServletRequest request) {
    
    ErrorResponse errorRes = ErrorResponse.builder()
      .timestamp(LocalDateTime.now())
      .status(HttpStatus.NOT_FOUND.value())
      .error("Not Found")
      .errorCode("RESOURCE_NOT_FOUND")
      .message(ex.getMessage())
      .path(request.getRequestURI())
      .build();

    return new ResponseEntity<>(errorRes, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(DuplicateResourceException.class)
  public ResponseEntity<ErrorResponse> handleDuplicateResource(
      DuplicateResourceException ex, HttpServletRequest request) {
    
    ErrorResponse errorRes = ErrorResponse.builder()
      .timestamp(LocalDateTime.now())
      .status(HttpStatus.CONFLICT.value())
      .error("Conflict")
      .errorCode("DUPLICATE_ENTRY")
      .message(ex.getMessage())
      .path(request.getRequestURI())
      .build();

    return new ResponseEntity<>(errorRes, HttpStatus.CONFLICT);
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<ErrorResponse> handleInvalidCredentials(
      InvalidCredentialsException ex, HttpServletRequest request) {
    
    ErrorResponse errorRes = ErrorResponse.builder()
      .timestamp(LocalDateTime.now())
      .status(HttpStatus.UNAUTHORIZED.value())
      .error("Unauthorized")
      .errorCode("INVALID_CREDENTIALS")
      .message("Invalid email or password")
      .path(request.getRequestURI())
      .build();

    return new ResponseEntity<>(errorRes, HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ErrorResponse> handleRuntimeException(
      RuntimeException ex, HttpServletRequest request) {
    
    logger.error("Runtime exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
    
    ErrorResponse errorRes = ErrorResponse.builder()
      .timestamp(LocalDateTime.now())
      .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
      .error("Internal Server Error")
      .errorCode("INTERNAL_ERROR")
      .message(ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred")
      .path(request.getRequestURI())
      .build();

    return new ResponseEntity<>(errorRes, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(
      Exception ex, HttpServletRequest request) {
    
    String errorId = UUID.randomUUID().toString().substring(0, 8);
    logger.error("Error [{}] at {}: {}", errorId, request.getRequestURI(), ex.getMessage(), ex);

    ErrorResponse errorRes = ErrorResponse.builder()
      .timestamp(LocalDateTime.now())
      .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
      .error("Internal Server Error")
      .errorCode("INTERNAL_ERROR")
      .message("An internal error occurred. Please try again later.")
      .path(request.getRequestURI())
      .build();

    return new ResponseEntity<>(errorRes, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}