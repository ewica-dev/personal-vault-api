package com.ewicadev.personalvaultapi.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.ewicadev.personalvaultapi.dto.NoteRequest;
import com.ewicadev.personalvaultapi.entity.Note;
import com.ewicadev.personalvaultapi.repository.NoteRepository;

@Service
public class NoteService {

  private final NoteRepository noteRepository;

  public NoteService(NoteRepository noteRepository) {
      this.noteRepository = noteRepository;
  }

  public Note createNote(NoteRequest request) {
    Note note = new Note();
    note.setTitle(request.getTitle());
    note.setContent(request.getContent());
    
    return noteRepository.save(note);
  }

  public Note updateNote(Long id, NoteRequest request) {
    // Safely attempt to find the note first. 
    // If it doesn't exist, throw an exception so the API doesn't crash silently.
    Note existingNote = noteRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Note not found with ID: " + id));

    // Update only the fields we allow
    existingNote.setTitle(request.getTitle());
    existingNote.setContent(request.getContent());

    return noteRepository.save(existingNote);
  }

  public List<Note> getAllNotes() {
    return noteRepository.findAll();
  }

  public Optional<Note> getNoteById(Long id) {
    return noteRepository.findById(id);
  }

  // Delete: Verify it exists before trying to delete
  public void deleteNote(Long id) {
    if (!noteRepository.existsById(id)) {
        throw new RuntimeException("Note not found with ID: " + id);
    }
    noteRepository.deleteById(id);
  }  
}