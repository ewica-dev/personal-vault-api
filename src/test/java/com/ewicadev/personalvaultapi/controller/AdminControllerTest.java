package com.ewicadev.personalvaultapi.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

  @InjectMocks
  private AdminController adminController;

  @Test
  @DisplayName("test endpoint returns 200")
  void testEndpointReturns200() {
    ResponseEntity<String> result = adminController.test();
    
    assertThat(result.getStatusCode().value()).isEqualTo(200);
    assertThat(result.getBody()).isEqualTo("Admin endpoint accessible");
  }
}