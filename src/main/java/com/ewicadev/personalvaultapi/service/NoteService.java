package com.ewicadev.personalvaultapi.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ewicadev.personalvaultapi.dto.note.NoteRequest;
import com.ewicadev.personalvaultapi.entity.Note;
import com.ewicadev.personalvaultapi.exception.ResourceNotFoundException;
import com.ewicadev.personalvaultapi.repository.NoteRepository;
import com.ewicadev.personalvaultapi.util.HtmlSanitizerUtil;

@Service
public class NoteService {

  private final NoteRepository noteRepository;

  public NoteService(NoteRepository noteRepository) {
      this.noteRepository = noteRepository;
  }

  public Note createNote(NoteRequest request, Long userId) {
    Note note = new Note();
    note.setTitle(HtmlSanitizerUtil.sanitize(request.getTitle()));
    note.setContent(HtmlSanitizerUtil.sanitize(request.getContent()));
    note.setUserId(userId);
    
    return noteRepository.save(note);
  }

  public Note updateNote(Long id, NoteRequest request, Long userId) {
    Note existingNote = noteRepository.findByIdAndUserId(id, userId)
        .orElseThrow(() -> new ResourceNotFoundException("Note not found with ID: " + id));

    existingNote.setTitle(HtmlSanitizerUtil.sanitize(request.getTitle()));
    existingNote.setContent(HtmlSanitizerUtil.sanitize(request.getContent()));

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