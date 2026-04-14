package com.ewicadev.personalvaultapi.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ewicadev.personalvaultapi.dto.note.NoteRequest;
import com.ewicadev.personalvaultapi.entity.Note;
import com.ewicadev.personalvaultapi.exception.ResourceNotFoundException;
import com.ewicadev.personalvaultapi.repository.NoteRepository;
import com.ewicadev.personalvaultapi.util.TextUtil;

/**
 * Service for managing user notes.
 * 
 * <p>Notes are stored as plain text, not HTML. Users can store content like
 * "2 &lt; 3" or code snippets without restriction.
 * 
 * <p>XSS protection is handled at render time by the frontend:
 * <ul>
 *   <li>Escape output when displaying note content</li>
 *   <li>Use CSS {@code white-space: pre-wrap} to preserve formatting</li>
 * </ul>
 */
@Service
public class NoteService {

  private final NoteRepository noteRepository;

  public NoteService(NoteRepository noteRepository) {
      this.noteRepository = noteRepository;
  }

  public Note createNote(NoteRequest request, Long userId) {
    Note note = new Note();
    note.setTitle(TextUtil.normalizeTitle(request.getTitle()));
    note.setContent(TextUtil.normalizeContent(request.getContent()));
    note.setUserId(userId);
    
    return noteRepository.save(note);
  }

  public Note updateNote(Long id, NoteRequest request, Long userId) {
    Note existingNote = noteRepository.findByIdAndUserId(id, userId)
        .orElseThrow(() -> new ResourceNotFoundException("Note not found with ID: " + id));

    existingNote.setTitle(TextUtil.normalizeTitle(request.getTitle()));
    existingNote.setContent(TextUtil.normalizeContent(request.getContent()));

    return noteRepository.save(existingNote);
  }

  public List<Note> getAllNotesByUser(Long userId) {
    return noteRepository.findByUserId(userId);
  }

  public Page<Note> getNotesByUser(Long userId, Pageable pageable) {
    return noteRepository.findByUserId(userId, pageable);
  }

  public Note getNoteById(Long id, Long userId) {
    return noteRepository.findByIdAndUserId(id, userId)
        .orElseThrow(() -> new ResourceNotFoundException("Note not found with ID: " + id));
  }

  public void deleteNote(Long id, Long userId) {
    Note note = noteRepository.findByIdAndUserId(id, userId)
        .orElseThrow(() -> new ResourceNotFoundException("Note not found with ID: " + id));
    noteRepository.delete(note);
  }  
}