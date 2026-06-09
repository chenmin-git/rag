package com.example.campusrag.web;

import jakarta.validation.constraints.NotBlank;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.campusrag.common.ApiResponse;
import com.example.campusrag.domain.UserAccount;
import com.example.campusrag.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<UserAccount> login(@RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request.username(), request.password()));
    }

    @GetMapping("/me")
    public ApiResponse<UserAccount> me(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ApiResponse.ok(authService.requireCurrentUser(userId));
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }
}
