package com.example.campusrag.web;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.campusrag.common.ApiResponse;
import com.example.campusrag.domain.OperationLog;
import com.example.campusrag.domain.QaRecord;
import com.example.campusrag.domain.SystemConfig;
import com.example.campusrag.domain.UserAccount;
import com.example.campusrag.domain.Feedback;
import com.example.campusrag.repository.FeedbackRepository;
import com.example.campusrag.repository.OperationLogRepository;
import com.example.campusrag.repository.QaRecordRepository;
import com.example.campusrag.repository.UserAccountRepository;
import com.example.campusrag.service.AuthService;
import com.example.campusrag.service.ConfigService;
import com.example.campusrag.service.UserService;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final ConfigService configService;
    private final UserService userService;
    private final OperationLogRepository logRepository;
    private final FeedbackRepository feedbackRepository;
    private final QaRecordRepository qaRecordRepository;
    private final UserAccountRepository userRepository;
    private final AuthService authService;

    public AdminController(
            ConfigService configService,
            UserService userService,
            OperationLogRepository logRepository,
            FeedbackRepository feedbackRepository,
            QaRecordRepository qaRecordRepository,
            UserAccountRepository userRepository,
            AuthService authService) {
        this.configService = configService;
        this.userService = userService;
        this.logRepository = logRepository;
        this.feedbackRepository = feedbackRepository;
        this.qaRecordRepository = qaRecordRepository;
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @GetMapping("/settings")
    public ApiResponse<List<SystemConfig>> settings(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        authService.requireAnyRole(userId, "SYSTEM_ADMIN");
        return ApiResponse.ok(configService.all());
    }

    @PutMapping("/settings")
    public ApiResponse<List<SystemConfig>> updateSettings(
            @RequestBody Map<String, String> values,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        authService.requireAnyRole(userId, "SYSTEM_ADMIN");
        return ApiResponse.ok(configService.update(values));
    }

    @GetMapping("/users")
    public ApiResponse<List<UserAccount>> users(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        authService.requireAnyRole(userId, "SYSTEM_ADMIN");
        return ApiResponse.ok(userService.list());
    }

    @org.springframework.web.bind.annotation.PostMapping("/users")
    public ApiResponse<UserAccount> createUser(
            @RequestBody UserRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        authService.requireAnyRole(userId, "SYSTEM_ADMIN");
        return ApiResponse.ok(userService.create(
                request.username(),
                request.password(),
                request.displayName(),
                request.role(),
                request.department()));
    }

    @PutMapping("/users/{id}")
    public ApiResponse<UserAccount> updateUser(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestBody UserRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        authService.requireAnyRole(userId, "SYSTEM_ADMIN");
        return ApiResponse.ok(userService.update(
                id,
                request.displayName(),
                request.role(),
                request.department(),
                request.enabled()));
    }

    @GetMapping("/logs")
    public ApiResponse<List<OperationLog>> logs(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        authService.requireAnyRole(userId, "SYSTEM_ADMIN");
        return ApiResponse.ok(logRepository.findTop80ByOrderByCreatedAtDesc());
    }

    @GetMapping("/feedback")
    public ApiResponse<List<FeedbackView>> feedback(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        authService.requireAnyRole(userId, "SYSTEM_ADMIN", "DEPARTMENT_ADMIN");
        List<Feedback> items = feedbackRepository.findTop80ByOrderByCreatedAtDesc();
        Map<Long, QaRecord> qaMap = qaRecordRepository.findAllById(
                items.stream().map(Feedback::getQaRecordId).toList()).stream()
                .collect(Collectors.toMap(QaRecord::getId, Function.identity()));
        Map<Long, UserAccount> userMap = userRepository.findAllById(
                items.stream().map(Feedback::getUserId).toList()).stream()
                .collect(Collectors.toMap(UserAccount::getId, Function.identity()));
        return ApiResponse.ok(items.stream()
                .map(item -> {
                    QaRecord qa = qaMap.get(item.getQaRecordId());
                    UserAccount user = userMap.get(item.getUserId());
                    return new FeedbackView(
                            item.getId(),
                            item.getQaRecordId(),
                            item.getUserId(),
                            user == null ? null : user.getDisplayName(),
                            qa == null ? null : qa.getQuestion(),
                            qa == null ? null : qa.getAnswer(),
                            item.getType(),
                            item.getComment(),
                            item.getCreatedAt());
                })
                .toList());
    }

    public record UserRequest(String username, String password, String displayName, String role, String department, Boolean enabled) {
    }

    public record FeedbackView(
            Long id,
            Long qaRecordId,
            Long userId,
            String userName,
            String question,
            String answer,
            String type,
            String comment,
            java.time.LocalDateTime createdAt) {
    }
}
