package com.ewicadev.personalvaultapi.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ewicadev.personalvaultapi.service.NoteEncryptionService;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Component
@Converter
public class NoteEncryptionConverter implements AttributeConverter<String, String> {

  @Autowired
  private NoteEncryptionService encryptionService;

  public NoteEncryptionConverter() {
  }

  @Override
  public String convertToDatabaseColumn(String attribute) {
    if (attribute == null) {
      return null;
    }
    return encryptionService.encrypt(attribute);
  }

  @Override
  public String convertToEntityAttribute(String dbData) {
    if (dbData == null) {
      return null;
    }
    return encryptionService.decrypt(dbData);
  }
}