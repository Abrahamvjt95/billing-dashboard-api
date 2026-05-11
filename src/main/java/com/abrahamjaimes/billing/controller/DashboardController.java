package com.abrahamjaimes.billing.controller;

import com.abrahamjaimes.billing.dto.response.DashboardStatsResponse;
import com.abrahamjaimes.billing.entity.User;
import com.abrahamjaimes.billing.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Aggregated stats for the current user")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    @Operation(summary = "Get revenue and invoice stats")
    public DashboardStatsResponse stats(@AuthenticationPrincipal User user) {
        return dashboardService.getStats(user);
    }
}
