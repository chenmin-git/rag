package com.example.campusrag.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.campusrag.config.AppProperties;
import com.example.campusrag.domain.Feedback;
import com.example.campusrag.domain.QaRecord;
import com.example.campusrag.domain.UserAccount;
import com.example.campusrag.repository.FeedbackRepository;
import com.example.campusrag.repository.QaRecordRepository;
import com.example.campusrag.service.VectorStore.SearchHit;
import com.example.campusrag.service.VectorStore.SearchRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class RagService {
    private final AppProperties properties;
    private final ConfigService configService;
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final RerankService rerankService;
    private final PromptBuilder promptBuilder;
    private final RoutingLlmClient llmClient;
    private final QaRecordRepository qaRecordRepository;
    private final FeedbackRepository feedbackRepository;
    private final ObjectMapper objectMapper;

    public RagService(
            AppProperties properties,
            ConfigService configService,
            EmbeddingService embeddingService,
            VectorStore vectorStore,
            RerankService rerankService,
            PromptBuilder promptBuilder,
            RoutingLlmClient llmClient,
            QaRecordRepository qaRecordRepository,
            FeedbackRepository feedbackRepository,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.configService = configService;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.rerankService = rerankService;
        this.promptBuilder = promptBuilder;
        this.llmClient = llmClient;
        this.qaRecordRepository = qaRecordRepository;
        this.feedbackRepository = feedbackRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AskResult ask(UserAccount user, String question, Integer topK, Double threshold) {
        Instant start = Instant.now();
        String cleaned = question == null ? "" : question.trim();
        if (cleaned.isBlank()) {
            throw new IllegalArgumentException("问题不能为空");
        }
        int defaultTopK = configService.getInt("rag.topK", properties.getRag().getTopK());
        double defaultThreshold = configService.getDouble("rag.similarityThreshold", properties.getRag().getSimilarityThreshold());
        int finalTopK = topK == null || topK <= 0 ? defaultTopK : Math.min(topK, 12);
        double finalThreshold = threshold == null ? defaultThreshold : threshold;
        float[] query = embeddingService.embed(cleaned);
        List<SearchHit> hits = rerankService.rerank(cleaned, vectorStore.search(query, new SearchRequest(finalTopK, finalThreshold, user)));
        String prompt = promptBuilder.build(cleaned, hits);
        String answer = llmClient.generate(cleaned, prompt, hits);
        String answerMode = llmClient.status().mode();
        long latency = Duration.between(start, Instant.now()).toMillis();
        QaRecord record = new QaRecord();
        record.setUserId(user.getId());
        record.setUserName(user.getDisplayName());
        record.setQuestion(cleaned);
        record.setAnswer(answer);
        record.setSourcesJson(toJson(hits));
        record.setLatencyMs(latency);
        record = qaRecordRepository.save(record);
        return new AskResult(record.getId(), cleaned, answer, hits, latency, answerMode);
    }

    @Transactional(readOnly = true)
    public List<QaRecord> history(UserAccount user) {
        if ("SYSTEM_ADMIN".equalsIgnoreCase(user.getRole())) {
            return qaRecordRepository.findTop8ByOrderByCreatedAtDesc();
        }
        return qaRecordRepository.findTop20ByUserIdOrderByCreatedAtDesc(user.getId());
    }

    @Transactional
    public Feedback feedback(UserAccount user, Long qaRecordId, String type, String comment) {
        Feedback feedback = new Feedback();
        feedback.setQaRecordId(qaRecordId);
        feedback.setUserId(user.getId());
        feedback.setType(type);
        feedback.setComment(comment);
        return feedbackRepository.save(feedback);
    }

    private String toJson(List<SearchHit> hits) {
        try {
            return objectMapper.writeValueAsString(hits);
        } catch (JsonProcessingException exception) {
            return "[]";
        }
    }

    public record AskResult(Long id, String question, String answer, List<SearchHit> sources, long latencyMs, String answerMode) {
    }
}
