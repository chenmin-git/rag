package com.example.campusrag.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.campusrag.domain.DocumentStatus;
import com.example.campusrag.domain.KnowledgeDocument;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {
    List<KnowledgeDocument> findTop8ByOrderByCreatedAtDesc();

    List<KnowledgeDocument> findByOriginalName(String originalName);

    long countByStatus(DocumentStatus status);

    @Query("select d.department, count(d) from KnowledgeDocument d group by d.department")
    List<Object[]> countByDepartment();
}
