package com.example.campusrag.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.campusrag.common.ApiResponse;
import com.example.campusrag.service.AuthService;
import com.example.campusrag.service.DashboardService;
import com.example.campusrag.service.DashboardService.Dashboard;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;
    private final AuthService authService;

    public DashboardController(DashboardService dashboardService, AuthService authService) {
        this.dashboardService = dashboardService;
        this.authService = authService;
    }

    @GetMapping
    public ApiResponse<Dashboard> dashboard(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        authService.requireCurrentUser(userId);
        return ApiResponse.ok(dashboardService.dashboard());
    }
}
