package com.ewicadev.personalvaultapi.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ewicadev.personalvaultapi.entity.Note;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {
  List<Note> findByUserId(Long userId);
  Page<Note> findByUserId(Long userId, Pageable pageable);
  Optional<Note> findByIdAndUserId(Long id, Long userId);
}