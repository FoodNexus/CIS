package com.civicplatform.controller;

import com.civicplatform.dto.response.DashboardStatsResponse;
import com.civicplatform.entity.User;
import com.civicplatform.repository.UserRepository;
import com.civicplatform.service.DashboardService;
import com.civicplatform.service.MlServiceClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin management APIs")
public class AdminController {

    private final DashboardService dashboardService;
    private final MlServiceClient mlServiceClient;
    private final UserRepository userRepository;

    @Operation(summary = "Get dashboard statistics")
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        DashboardStatsResponse response = dashboardService.getDashboardStats();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Trigger ML model retraining (Python service)")
    @PostMapping("/ml/retrain")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> triggerMlRetrain(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!user.isAdmin()) {
            return ResponseEntity.status(403).build();
        }
        mlServiceClient.triggerRetrain();
        return ResponseEntity.ok(Map.of(
                "status", "accepted",
                "message", "Retraining triggered"
        ));
    }
}
