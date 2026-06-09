package com.example.campusrag.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.campusrag.domain.KnowledgeChunk;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, Long> {
    List<KnowledgeChunk> findByDocumentIdOrderByChunkNoAsc(Long documentId);

    void deleteByDocumentId(Long documentId);
}
