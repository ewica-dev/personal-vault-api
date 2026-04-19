package com.ewicadev.personalvaultapi.service;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

import com.ewicadev.personalvaultapi.config.EncryptionProperties;
import com.ewicadev.personalvaultapi.exception.EncryptionException;

@Service
public class NoteEncryptionService {

  private static final String ALGORITHM = "AES/GCM/NoPadding";
  private static final int GCM_IV_LENGTH = 12;
  private static final int GCM_TAG_LENGTH = 128;

  private final EncryptionProperties encryptionProperties;
  private final SecureRandom secureRandom;

  public NoteEncryptionService(EncryptionProperties encryptionProperties) {
    this.encryptionProperties = encryptionProperties;
    this.secureRandom = new SecureRandom();
  }

  public String encrypt(String plaintext) {
    if (plaintext == null) {
      return null;
    }

    try {
      byte[] keyBytes = encryptionProperties.getKeyBytes();
      SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

      byte[] iv = new byte[GCM_IV_LENGTH];
      secureRandom.nextBytes(iv);

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

      byte[] plaintextBytes = plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8);
      byte[] ciphertext = cipher.doFinal(plaintextBytes);

      ByteBuffer combined = ByteBuffer.allocate(iv.length + ciphertext.length);
      combined.put(iv);
      combined.put(ciphertext);

      return Base64.getEncoder().encodeToString(combined.array());
    } catch (Exception e) {
      throw new EncryptionException("Failed to encrypt data", e);
    }
  }

  public String decrypt(String encryptedData) {
    if (encryptedData == null) {
      return null;
    }

    try {
      byte[] keyBytes = encryptionProperties.getKeyBytes();
      SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

      byte[] combined = Base64.getDecoder().decode(encryptedData);

      ByteBuffer buffer = ByteBuffer.wrap(combined);
      byte[] iv = new byte[GCM_IV_LENGTH];
      buffer.get(iv);
      byte[] ciphertext = new byte[buffer.remaining()];
      buffer.get(ciphertext);

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

      byte[] plaintextBytes = cipher.doFinal(ciphertext);
      return new String(plaintextBytes, java.nio.charset.StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new EncryptionException("Failed to decrypt data", e);
    }
  }
}