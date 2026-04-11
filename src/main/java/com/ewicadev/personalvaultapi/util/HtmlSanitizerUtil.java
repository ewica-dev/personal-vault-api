package com.ewicadev.personalvaultapi.util;

import java.util.regex.Pattern;

public class HtmlSanitizerUtil {

  private static final String[] DANGEROUS_PATTERNS = {
    "<script",
    "</script>",
    "javascript:",
    "onerror=",
    "onload=",
    "<iframe",
    "</iframe>",
    "<object",
    "</object>",
    "<embed",
    "</embed>",
    "expression(",
    "vbscript:",
    "data:text/html"
  };

  public static String sanitize(String input) {
    if (input == null || input.isBlank()) {
      return input;
    }

    String sanitized = input;
    for (String pattern : DANGEROUS_PATTERNS) {
      sanitized = sanitized.replaceAll("(?i)" + Pattern.quote(pattern), "");
    }

    return sanitized.trim();
  }

  public static boolean containsDangerousInput(String input) {
    if (input == null || input.isBlank()) {
      return false;
    }

    String lowerInput = input.toLowerCase();
    for (String pattern : DANGEROUS_PATTERNS) {
      if (lowerInput.contains(pattern.toLowerCase())) {
        return true;
      }
    }

    return false;
  }
}