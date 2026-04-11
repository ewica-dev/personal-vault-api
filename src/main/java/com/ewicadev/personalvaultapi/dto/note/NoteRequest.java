package com.ewicadev.personalvaultapi.dto.note;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NoteRequest {
  @NotBlank(message = "Title is required")
  @Size(max = 100, message = "Title must be under 100 characters")
  @Pattern(regexp = "^[^<>]+$", message = "Title must not contain HTML tags")
  private String title;

  @NotBlank(message = "Content cannot be empty")
  @Size(max = 5000, message = "Content is too long")
  @Pattern(regexp = "^[^<>]+$", message = "Content must not contain HTML tags")
  private String content;
}
