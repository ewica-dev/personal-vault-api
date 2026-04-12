package com.ewicadev.personalvaultapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.ewicadev.personalvaultapi.dto.note.NoteRequest;
import com.ewicadev.personalvaultapi.entity.Note;
import com.ewicadev.personalvaultapi.exception.ResourceNotFoundException;
import com.ewicadev.personalvaultapi.repository.NoteRepository;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

  @Mock
  private NoteRepository noteRepository;

  @InjectMocks
  private NoteService noteService;

  private NoteRequest noteRequest;
  private Note note;
  private Long userId;
  private Long noteId;

  @BeforeEach
  void setUp() {
    noteRequest = new NoteRequest();
    noteRequest.setTitle("Test Title");
    noteRequest.setContent("Test Content");

    note = new Note();
    note.setId(1L);
    note.setTitle("Test Title");
    note.setContent("Test Content");
    note.setUserId(1L);
    note.setCreatedAt(LocalDateTime.now());
    note.setUpdatedAt(LocalDateTime.now());

    userId = 1L;
    noteId = 1L;
  }

  @Test
  void createNoteValidRequestReturnsNote() {
    when(noteRepository.save(any(Note.class))).thenReturn(note);

    Note result = noteService.createNote(noteRequest, userId);

    assertThat(result).isNotNull();
    assertThat(result.getTitle()).isEqualTo("Test Title");
    assertThat(result.getContent()).isEqualTo("Test Content");
    assertThat(result.getUserId()).isEqualTo(userId);
    verify(noteRepository).save(any(Note.class));
  }

  @Test
  void createNoteSanitizesHtmlTagsStripped() {
    noteRequest.setTitle("<script>alert('xss')</script>Test Title");
    noteRequest.setContent("<img onerror=alert(1) src=x>Test Content");

    Note sanitizedNote = new Note();
    sanitizedNote.setId(1L);
    sanitizedNote.setTitle("Test Title");
    sanitizedNote.setContent("Test Content");
    sanitizedNote.setUserId(userId);
    sanitizedNote.setCreatedAt(LocalDateTime.now());
    sanitizedNote.setUpdatedAt(LocalDateTime.now());

    when(noteRepository.save(any(Note.class))).thenReturn(sanitizedNote);

    Note result = noteService.createNote(noteRequest, userId);

    assertThat(result.getTitle()).isEqualTo("Test Title");
    assertThat(result.getContent()).isEqualTo("Test Content");
  }

  @Test
  void updateNoteExistingNoteReturnsUpdatedNote() {
    NoteRequest updateRequest = new NoteRequest();
    updateRequest.setTitle("Updated Title");
    updateRequest.setContent("Updated Content");

    Note updatedNote = new Note();
    updatedNote.setId(noteId);
    updatedNote.setTitle("Updated Title");
    updatedNote.setContent("Updated Content");
    updatedNote.setUserId(userId);
    updatedNote.setCreatedAt(LocalDateTime.now());
    updatedNote.setUpdatedAt(LocalDateTime.now());

    when(noteRepository.findByIdAndUserId(noteId, userId)).thenReturn(Optional.of(note));
    when(noteRepository.save(any(Note.class))).thenReturn(updatedNote);

    Note result = noteService.updateNote(noteId, updateRequest, userId);

    assertThat(result).isNotNull();
    assertThat(result.getTitle()).isEqualTo("Updated Title");
    assertThat(result.getContent()).isEqualTo("Updated Content");
    verify(noteRepository).findByIdAndUserId(noteId, userId);
    verify(noteRepository).save(any(Note.class));
  }

  @Test
  void updateNoteNoteNotFoundThrowsException() {
    NoteRequest updateRequest = new NoteRequest();
    updateRequest.setTitle("Updated Title");
    updateRequest.setContent("Updated Content");

    when(noteRepository.findByIdAndUserId(noteId, userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> noteService.updateNote(noteId, updateRequest, userId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Note not found with ID");

    verify(noteRepository).findByIdAndUserId(noteId, userId);
    verify(noteRepository, never()).save(any(Note.class));
  }

  @Test
  void getAllNotesByUserReturnsNotesList() {
    List<Note> notes = List.of(note);

    when(noteRepository.findByUserId(userId)).thenReturn(notes);

    List<Note> result = noteService.getAllNotesByUser(userId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getId()).isEqualTo(noteId);
    verify(noteRepository).findByUserId(userId);
  }

  @Test
  void getNotesByUserWithPageableReturnsPagedNotes() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<Note> page = new PageImpl<>(List.of(note), pageable, 1);

    when(noteRepository.findByUserId(userId, pageable)).thenReturn(page);

    Page<Note> result = noteService.getNotesByUser(userId, pageable);

    assertThat(result).hasSize(1);
    assertThat(result.getContent().get(0).getId()).isEqualTo(noteId);
    verify(noteRepository).findByUserId(userId, pageable);
  }

  @Test
  void getNoteByIdExistingNoteReturnsNote() {
    when(noteRepository.findByIdAndUserId(noteId, userId)).thenReturn(Optional.of(note));

    Note result = noteService.getNoteById(noteId, userId);

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(noteId);
    verify(noteRepository).findByIdAndUserId(noteId, userId);
  }

  @Test
  void getNoteByIdNoteNotFoundThrowsException() {
    when(noteRepository.findByIdAndUserId(noteId, userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> noteService.getNoteById(noteId, userId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Note not found with ID");

    verify(noteRepository).findByIdAndUserId(noteId, userId);
  }

  @Test
  void deleteNoteExistingNoteDeletesSuccessfully() {
    when(noteRepository.findByIdAndUserId(noteId, userId)).thenReturn(Optional.of(note));

    noteService.deleteNote(noteId, userId);

    verify(noteRepository).findByIdAndUserId(noteId, userId);
    verify(noteRepository).delete(note);
  }

  @Test
  void deleteNoteNoteNotFoundThrowsException() {
    when(noteRepository.findByIdAndUserId(noteId, userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> noteService.deleteNote(noteId, userId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Note not found with ID");

    verify(noteRepository).findByIdAndUserId(noteId, userId);
    verify(noteRepository, never()).delete(any(Note.class));
  }
}