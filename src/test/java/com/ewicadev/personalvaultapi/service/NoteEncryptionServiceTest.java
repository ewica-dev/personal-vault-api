package com.ewicadev.personalvaultapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ewicadev.personalvaultapi.config.EncryptionProperties;
import com.ewicadev.personalvaultapi.exception.EncryptionException;

class NoteEncryptionServiceTest {

  private NoteEncryptionService encryptionService;

  @BeforeEach
  void setUp() {
    byte[] keyBytes = new byte[32];
    for (int i = 0; i < 32; i++) {
      keyBytes[i] = (byte) i;
    }
    String testKey = Base64.getEncoder().encodeToString(keyBytes);
    EncryptionProperties encryptionProperties = new EncryptionProperties(testKey);
    encryptionService = new NoteEncryptionService(encryptionProperties);
  }

  @Test
  void encryptEmptyStringReturnsEncryptedValue() {
    String encrypted = encryptionService.encrypt("");
    assertThat(encrypted).isNotNull();
    assertThat(encrypted).isNotEmpty();
  }

  @Test
  void encryptNormalTextReturnsEncryptedValue() {
    String plaintext = "Hello, World!";

    String encrypted = encryptionService.encrypt(plaintext);

    assertThat(encrypted).isNotNull();
    assertThat(encrypted).isNotEqualTo(plaintext);
  }

  @Test
  void encryptLongContentReturnsEncryptedValue() {
    StringBuilder longContent = new StringBuilder();
    for (int i = 0; i < 1000; i++) {
      longContent.append("Lorem ipsum dolor sit amet. ");
    }
    String plaintext = longContent.toString();

    String encrypted = encryptionService.encrypt(plaintext);

    assertThat(encrypted).isNotNull();
    assertThat(encrypted.length()).isGreaterThan(plaintext.length());
  }

  @Test
  void encryptUnicodeContentReturnsEncryptedValue() {
    String plaintext = "Hello \u4e16\u754c! \u3042\u308a\u304c\u3068";

    String encrypted = encryptionService.encrypt(plaintext);

    assertThat(encrypted).isNotNull();
    String decrypted = encryptionService.decrypt(encrypted);
    assertThat(decrypted).isEqualTo(plaintext);
  }

  @Test
  void decryptTamperedCiphertextThrowsException() {
    String plaintext = "Secret message";
    String encrypted = encryptionService.encrypt(plaintext);

    String tampered = encrypted.substring(0, encrypted.length() - 2) + "XX";

    assertThatThrownBy(() -> encryptionService.decrypt(tampered))
        .isInstanceOf(EncryptionException.class);
  }

  @Test
  void decryptTruncatedDataThrowsException() {
    String plaintext = "Secret message";
    String encrypted = encryptionService.encrypt(plaintext);

    String truncated = encrypted.substring(0, encrypted.length() - 10);

    assertThatThrownBy(() -> encryptionService.decrypt(truncated))
        .isInstanceOf(EncryptionException.class);
  }

  @Test
  void decryptInvalidBase64ThrowsException() {
    String invalidBase64 = "NOT_VALID_BASE64!!";

    assertThatThrownBy(() -> encryptionService.decrypt(invalidBase64))
        .isInstanceOf(EncryptionException.class);
  }

  @Test
  void encryptingSamePlaintextProducesDifferentOutputs() {
    String plaintext = "Hello, World!";

    String encrypted1 = encryptionService.encrypt(plaintext);
    String encrypted2 = encryptionService.encrypt(plaintext);

    assertThat(encrypted1).isNotEqualTo(encrypted2);
    assertThat(encryptionService.decrypt(encrypted1)).isEqualTo(plaintext);
    assertThat(encryptionService.decrypt(encrypted2)).isEqualTo(plaintext);
  }

  @Test
  void encryptNullInputReturnsNull() {
    String result = encryptionService.encrypt(null);
    assertThat(result).isNull();
  }

  @Test
  void decryptNullInputReturnsNull() {
    String result = encryptionService.decrypt(null);
    assertThat(result).isNull();
  }

  @Test
  void roundTripEmptyStringReturnsOriginal() {
    String plaintext = "";

    String encrypted = encryptionService.encrypt(plaintext);
    String decrypted = encryptionService.decrypt(encrypted);

    assertThat(decrypted).isEqualTo(plaintext);
  }

  @Test
  void roundTripNormalTextReturnsOriginal() {
    String plaintext = "Hello, World!";

    String encrypted = encryptionService.encrypt(plaintext);
    String decrypted = encryptionService.decrypt(encrypted);

    assertThat(decrypted).isEqualTo(plaintext);
  }

  @Test
  void roundTripLongContentReturnsOriginal() {
    StringBuilder longContent = new StringBuilder();
    for (int i = 0; i < 1000; i++) {
      longContent.append("Lorem ipsum dolor sit amet. ");
    }
    String plaintext = longContent.toString();

    String encrypted = encryptionService.encrypt(plaintext);
    String decrypted = encryptionService.decrypt(encrypted);

    assertThat(decrypted).isEqualTo(plaintext);
  }

  @Test
  void roundTripUnicodeReturnsOriginal() {
    String plaintext = "Hello \u4e16\u754c! \u3042\u308a\u304c\u3068";

    String encrypted = encryptionService.encrypt(plaintext);
    String decrypted = encryptionService.decrypt(encrypted);

    assertThat(decrypted).isEqualTo(plaintext);
  }

  @Test
  void roundTripSpecialCharactersReturnsOriginal() {
    String plaintext = "Line1\nLine2\tTab\r\nWindows\r\nNull: \u0000";

    String encrypted = encryptionService.encrypt(plaintext);
    String decrypted = encryptionService.decrypt(encrypted);

    assertThat(decrypted).isEqualTo(plaintext);
  }
}