package com.ewicadev.personalvaultapi.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ewicadev.personalvaultapi.dto.NoteRequest;
import com.ewicadev.personalvaultapi.entity.Note;
import com.ewicadev.personalvaultapi.service.NoteService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/notes")
public class NoteController {

  private final NoteService noteService;

  public NoteController(NoteService noteService) {
      this.noteService = noteService;
  }

  @PostMapping
  public ResponseEntity<Note> createNote(@Valid @RequestBody NoteRequest request) {
      Note savedNote = noteService.createNote(request);
      return new ResponseEntity<>(savedNote, HttpStatus.CREATED);
  }

  @PutMapping("/{id}")
  public ResponseEntity<Note> updateNote(@PathVariable Long id, @Valid @RequestBody NoteRequest request) {
      Note updatedNote = noteService.updateNote(id, request);
      return ResponseEntity.ok(updatedNote);
  }

  @GetMapping
  public ResponseEntity<List<Note>> getAllNotes() {
      return ResponseEntity.ok(noteService.getAllNotes());
  }

  @GetMapping("/{id}")
  public ResponseEntity<Note> getNoteById(@PathVariable Long id) {
      return noteService.getNoteById(id)
              .map(ResponseEntity::ok)
              .orElse(ResponseEntity.notFound().build());
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteNote(@PathVariable Long id) {
      noteService.deleteNote(id);
      return ResponseEntity.noContent().build();
  }
}