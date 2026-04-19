package com.ewicadev.personalvaultapi.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.ewicadev.personalvaultapi.config.UserContext;
import com.ewicadev.personalvaultapi.dto.common.PagedResponse;
import com.ewicadev.personalvaultapi.dto.note.NoteRequest;
import com.ewicadev.personalvaultapi.dto.note.NoteResponse;
import com.ewicadev.personalvaultapi.dto.note.NoteSummaryResponse;
import com.ewicadev.personalvaultapi.entity.Note;
import com.ewicadev.personalvaultapi.entity.User;
import com.ewicadev.personalvaultapi.exception.ResourceNotFoundException;
import com.ewicadev.personalvaultapi.repository.UserRepository;
import com.ewicadev.personalvaultapi.service.NoteService;

@ExtendWith(MockitoExtension.class)
class NoteControllerTest {

  @Mock
  private NoteService noteService;

  @Mock
  private UserContext userContext;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private NoteController noteController;

  private User user;
  private Note note;
  private NoteRequest noteRequest;

  @BeforeEach
  void setUp() {
    user = new User();
    user.setId(1L);
    user.setEmail("test@example.com");

    note = new Note();
    note.setId(10L);
    note.setTitle("My Note");
    note.setContent("Hello world");
    note.setCreatedAt(LocalDateTime.now());
    note.setUpdatedAt(LocalDateTime.now());

    noteRequest = new NoteRequest();
    noteRequest.setTitle("My Note");
    noteRequest.setContent("Hello world");
  }

  @Test
  @DisplayName("createNote returns 201 with note response")
  void createNoteReturnsCreatedResponse() {
    when(userContext.getCurrentUserEmail()).thenReturn("test@example.com");
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
    when(noteService.createNote(noteRequest, 1L)).thenReturn(note);

    ResponseEntity<NoteResponse> response = noteController.createNote(noteRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getId()).isEqualTo(10L);
    assertThat(response.getBody().getTitle()).isEqualTo("My Note");
    assertThat(response.getBody().getContent()).isEqualTo("Hello world");

    verify(noteService).createNote(noteRequest, 1L);
  }

  @Test
  @DisplayName("getNoteById returns 200 with note response")
  void getNoteByIdReturnsOkResponse() {
    when(userContext.getCurrentUserEmail()).thenReturn("test@example.com");
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
    when(noteService.getNoteById(10L, 1L)).thenReturn(note);

    ResponseEntity<NoteResponse> response = noteController.getNoteById(10L);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getId()).isEqualTo(10L);
    assertThat(response.getBody().getTitle()).isEqualTo("My Note");

    verify(noteService).getNoteById(10L, 1L);
  }

  @Test
  @DisplayName("updateNote returns 200 with updated note response")
  void updateNoteReturnsOkResponse() {
    when(userContext.getCurrentUserEmail()).thenReturn("test@example.com");
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
    when(noteService.updateNote(10L, noteRequest, 1L)).thenReturn(note);

    ResponseEntity<NoteResponse> response = noteController.updateNote(10L, noteRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getId()).isEqualTo(10L);
    assertThat(response.getBody().getTitle()).isEqualTo("My Note");

    verify(noteService).updateNote(10L, noteRequest, 1L);
  }

  @Test
  @DisplayName("deleteNote returns 204 no content")
  void deleteNoteReturnsNoContent() {
    when(userContext.getCurrentUserEmail()).thenReturn("test@example.com");
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

    ResponseEntity<Void> response = noteController.deleteNote(10L);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(response.getBody()).isNull();

    verify(noteService).deleteNote(10L, 1L);
  }

  @Test
  @DisplayName("getAllNotes returns paged mapped response")
  void getAllNotesReturnsPagedResponse() {
    Page<Note> notePage = new PageImpl<>(List.of(note));

    when(userContext.getCurrentUserEmail()).thenReturn("test@example.com");
    when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
    when(noteService.getNotesByUser(eq(1L), any(Pageable.class))).thenReturn(notePage);

    ResponseEntity<PagedResponse<NoteSummaryResponse>> response = noteController.getAllNotes(0, 10);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getContent()).hasSize(1);
    assertThat(response.getBody().getContent().get(0).getId()).isEqualTo(10L);
    assertThat(response.getBody().getContent().get(0).getTitle()).isEqualTo("My Note");
    assertThat(response.getBody().getPage()).isEqualTo(0);
    assertThat(response.getBody().getSize()).isEqualTo(1);
  }

  @Test
  @DisplayName("throws ResourceNotFoundException when current user is missing")
  void throwsWhenCurrentUserNotFound() {
    when(userContext.getCurrentUserEmail()).thenReturn("missing@example.com");
    when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> noteController.getNoteById(10L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("User not authenticated");
  }
}