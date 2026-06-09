package com.example.campusrag.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.campusrag.domain.DocumentStatus;
import com.example.campusrag.repository.FeedbackRepository;
import com.example.campusrag.repository.KnowledgeChunkRepository;
import com.example.campusrag.repository.KnowledgeDocumentRepository;
import com.example.campusrag.repository.QaRecordRepository;

@Service
public class DashboardService {
    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final QaRecordRepository qaRecordRepository;
    private final FeedbackRepository feedbackRepository;

    public DashboardService(
            KnowledgeDocumentRepository documentRepository,
            KnowledgeChunkRepository chunkRepository,
            QaRecordRepository qaRecordRepository,
            FeedbackRepository feedbackRepository) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.qaRecordRepository = qaRecordRepository;
        this.feedbackRepository = feedbackRepository;
    }

    public Dashboard dashboard() {
        Map<String, Long> departments = new LinkedHashMap<>();
        for (Object[] row : documentRepository.countByDepartment()) {
            departments.put(String.valueOf(row[0]), (Long) row[1]);
        }
        Map<String, Long> feedback = new LinkedHashMap<>();
        for (Object[] row : feedbackRepository.countByType()) {
            feedback.put(String.valueOf(row[0]), (Long) row[1]);
        }
        return new Dashboard(
                documentRepository.count(),
                chunkRepository.count(),
                qaRecordRepository.count(),
                documentRepository.countByStatus(DocumentStatus.FAILED),
                Math.round(qaRecordRepository.averageLatencyMs()),
                departments,
                feedback,
                documentRepository.findTop8ByOrderByCreatedAtDesc(),
                qaRecordRepository.findTop8ByOrderByCreatedAtDesc());
    }

    public record Dashboard(
            long documentCount,
            long chunkCount,
            long qaCount,
            long failedTaskCount,
            long averageLatencyMs,
            Map<String, Long> departmentDistribution,
            Map<String, Long> feedbackDistribution,
            Object recentDocuments,
            Object recentQuestions) {
    }
}
