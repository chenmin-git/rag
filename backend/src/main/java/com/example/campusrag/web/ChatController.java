package com.example.campusrag.web;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.campusrag.common.ApiResponse;
import com.example.campusrag.domain.Feedback;
import com.example.campusrag.domain.QaRecord;
import com.example.campusrag.domain.UserAccount;
import com.example.campusrag.service.AuditService;
import com.example.campusrag.service.AuthService;
import com.example.campusrag.service.RagService;
import com.example.campusrag.service.RagService.AskResult;
import com.example.campusrag.service.RoutingLlmClient;
import com.example.campusrag.service.RoutingLlmClient.LlmStatus;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final RagService ragService;
    private final AuthService authService;
    private final AuditService auditService;
    private final RoutingLlmClient llmClient;

    public ChatController(RagService ragService, AuthService authService, AuditService auditService, RoutingLlmClient llmClient) {
        this.ragService = ragService;
        this.authService = authService;
        this.auditService = auditService;
        this.llmClient = llmClient;
    }

    @GetMapping("/status")
    public ApiResponse<LlmStatus> status(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        authService.requireCurrentUser(userId);
        return ApiResponse.ok(llmClient.status());
    }

    @PostMapping("/ask")
    public ApiResponse<AskResult> ask(
            @RequestBody AskRequest askRequest,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            HttpServletRequest request) {
        UserAccount user = authService.requireCurrentUser(userId);
        AskResult result = ragService.ask(user, askRequest.question(), askRequest.topK(), askRequest.threshold());
        auditService.log(user, "RAG_ASK", "chat", askRequest.question(), request.getRemoteAddr(), true);
        return ApiResponse.ok(result);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @RequestBody AskRequest askRequest,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            HttpServletRequest request) {
        SseEmitter emitter = new SseEmitter(90_000L);
        UserAccount user = authService.requireCurrentUser(userId);
        CompletableFuture.runAsync(() -> {
            try {
                AskResult result = ragService.ask(user, askRequest.question(), askRequest.topK(), askRequest.threshold());
                auditService.log(user, "RAG_STREAM", "chat", askRequest.question(), request.getRemoteAddr(), true);
                streamAnswer(emitter, result);
                emitter.send(SseEmitter.event().name("done").data(result));
                emitter.complete();
            } catch (Exception exception) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(exception.getMessage()));
                } catch (IOException ignored) {
                }
                emitter.completeWithError(exception);
            }
        });
        return emitter;
    }

    @GetMapping("/history")
    public ApiResponse<List<QaRecord>> history(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ApiResponse.ok(ragService.history(authService.requireCurrentUser(userId)));
    }

    @PostMapping("/{id}/feedback")
    public ApiResponse<Feedback> feedback(
            @PathVariable Long id,
            @RequestBody FeedbackRequest feedbackRequest,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        UserAccount user = authService.requireCurrentUser(userId);
        return ApiResponse.ok(ragService.feedback(user, id, feedbackRequest.type(), feedbackRequest.comment()));
    }

    private void streamAnswer(SseEmitter emitter, AskResult result) throws IOException, InterruptedException {
        String answer = result.answer();
        for (int index = 0; index < answer.length(); index += 8) {
            int end = Math.min(answer.length(), index + 8);
            emitter.send(SseEmitter.event().name("delta").data(answer.substring(index, end)));
            Thread.sleep(18L);
        }
    }

    public record AskRequest(@NotBlank String question, Integer topK, Double threshold) {
    }

    public record FeedbackRequest(@NotBlank String type, String comment) {
    }
}
