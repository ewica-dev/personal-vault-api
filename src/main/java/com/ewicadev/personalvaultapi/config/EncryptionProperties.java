package com.ewicadev.personalvaultapi.config;

import java.util.Base64;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "app.encryption")
@Getter
@Setter
public class EncryptionProperties {

  private String key;

  public EncryptionProperties() {
  }

  public EncryptionProperties(String key) {
    this.key = key;
  }

  @PostConstruct
  public void validate() {
    if (key == null || key.isBlank()) {
      throw new IllegalStateException("Encryption key is not configured. Set app.encryption.key property.");
    }

    try {
      byte[] decodedKey = Base64.getDecoder().decode(key);
      if (decodedKey.length != 32) {
        throw new IllegalStateException(
            "Encryption key must be exactly 32 bytes (256 bits) when decoded. "
                + "Current decoded length: "
                + decodedKey.length
                + " bytes. Provide a valid Base64-encoded 32-byte key.");
      }
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException(
          "Encryption key is not valid Base64. Please provide a valid Base64-encoded key.");
    }
  }

  public byte[] getKeyBytes() {
    return Base64.getDecoder().decode(key);
  }
}