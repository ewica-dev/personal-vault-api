package com.ewicadev.personalvaultapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ewicadev.personalvaultapi.entity.Note;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {
  // JpaRepository gives save(), findAll(), findById(), and deleteById()
}