package com.civicplatform.controller;

import com.civicplatform.dto.response.FeedResponse;
import com.civicplatform.entity.User;
import com.civicplatform.enums.UserType;
import com.civicplatform.repository.UserRepository;
import com.civicplatform.service.FeedRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recommendations")
@RequiredArgsConstructor
@Tag(name = "Recommendations", description = "ML-powered personalized feed")
public class RecommendationController {

    private final UserRepository userRepository;
    private final FeedRecommendationService feedRecommendationService;

    @Operation(summary = "Personalized feed (campaigns, projects, posts) from ML service")
    @GetMapping("/feed")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FeedResponse> getRecommendedFeed(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isAdmin() || user.getUserType() == UserType.DONOR) {
            return ResponseEntity.status(403).build();
        }

        FeedResponse response = feedRecommendationService.buildFeed(user);
        return ResponseEntity.ok(response);
    }
}
