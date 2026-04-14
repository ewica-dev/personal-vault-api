package com.ewicadev.personalvaultapi.util;

public class TextUtil {

  public static String normalizeTitle(String input) {
    if (input == null) {
      return null;
    }
    return input.trim().replaceAll("\\s+", " ");
  }

  public static String normalizeContent(String input) {
    if (input == null) {
      return null;
    }
    return input.replaceAll("\r\n", "\n").replaceAll("\r", "\n");
  }
}
