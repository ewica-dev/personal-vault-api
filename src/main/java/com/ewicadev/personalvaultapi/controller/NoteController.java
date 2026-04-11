package com.ewicadev.personalvaultapi.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ewicadev.personalvaultapi.config.UserContext;
import com.ewicadev.personalvaultapi.dto.common.PagedResponse;
import com.ewicadev.personalvaultapi.dto.note.NoteRequest;
import com.ewicadev.personalvaultapi.dto.note.NoteResponse;
import com.ewicadev.personalvaultapi.dto.note.NoteSummaryResponse;
import com.ewicadev.personalvaultapi.entity.Note;
import com.ewicadev.personalvaultapi.exception.ResourceNotFoundException;
import com.ewicadev.personalvaultapi.repository.UserRepository;
import com.ewicadev.personalvaultapi.service.NoteService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/notes")
public class NoteController {

  private final NoteService noteService;
  private final UserContext userContext;
  private final UserRepository userRepository;

  public NoteController(NoteService noteService, UserContext userContext, UserRepository userRepository) {
      this.noteService = noteService;
      this.userContext = userContext;
      this.userRepository = userRepository;
  }

  private Long getCurrentUserId() {
    String email = userContext.getCurrentUserEmail();
    return userRepository.findByEmail(email)
        .map(user -> user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("User not authenticated"));
  }

  @PostMapping
  public ResponseEntity<NoteResponse> createNote(@Valid @RequestBody NoteRequest request) {
      Note savedNote = noteService.createNote(request, getCurrentUserId());
      return new ResponseEntity<>(toNoteResponse(savedNote), HttpStatus.CREATED);
  }

  @PutMapping("/{id}")
  public ResponseEntity<NoteResponse> updateNote(@PathVariable Long id, @Valid @RequestBody NoteRequest request) {
      Note updatedNote = noteService.updateNote(id, request, getCurrentUserId());
      return ResponseEntity.ok(toNoteResponse(updatedNote));
  }

  @GetMapping
  public ResponseEntity<PagedResponse<NoteSummaryResponse>> getAllNotes(
          @RequestParam(defaultValue = "0") int page,
          @RequestParam(defaultValue = "10") int size) {
      Pageable pageable = PageRequest.of(page, size);
      Page<Note> notePage = noteService.getNotesByUser(getCurrentUserId(), pageable);
      
      PagedResponse<NoteSummaryResponse> response = PagedResponse.<NoteSummaryResponse>builder()
          .content(notePage.getContent().stream()
              .map(this::toNoteSummaryResponse)
              .toList())
          .page(notePage.getNumber())
          .size(notePage.getSize())
          .totalElements(notePage.getTotalElements())
          .totalPages(notePage.getTotalPages())
          .last(notePage.isLast())
          .build();
      
      return ResponseEntity.ok(response);
  }

  @GetMapping("/{id}")
  public ResponseEntity<NoteResponse> getNoteById(@PathVariable Long id) {
      Note note = noteService.getNoteById(id, getCurrentUserId());
      return ResponseEntity.ok(toNoteResponse(note));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteNote(@PathVariable Long id) {
      noteService.deleteNote(id, getCurrentUserId());
      return ResponseEntity.noContent().build();
  }

  private NoteResponse toNoteResponse(Note note) {
      return NoteResponse.builder()
          .id(note.getId())
          .title(note.getTitle())
          .content(note.getContent())
          .createdAt(note.getCreatedAt())
          .updatedAt(note.getUpdatedAt())
          .build();
  }

  private NoteSummaryResponse toNoteSummaryResponse(Note note) {
      return NoteSummaryResponse.builder()
          .id(note.getId())
          .title(note.getTitle())
          .createdAt(note.getCreatedAt())
          .updatedAt(note.getUpdatedAt())
          .build();
  }
}