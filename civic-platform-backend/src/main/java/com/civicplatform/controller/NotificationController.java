package com.civicplatform.controller;

import com.civicplatform.dto.response.NotificationResponse;
import com.civicplatform.entity.User;
import com.civicplatform.security.CurrentUserResolver;
import com.civicplatform.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notifications for signed-in users")
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserResolver currentUserResolver;

    @Operation(summary = "List notifications (newest first)")
    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        User user = getUser(authentication);
        Pageable p = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
        return ResponseEntity.ok(notificationService.listForUser(user.getId(), p));
    }

    @Operation(summary = "Unread notification count")
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(Authentication authentication) {
        User user = getUser(authentication);
        long n = notificationService.unreadCount(user.getId());
        return ResponseEntity.ok(Map.of("count", n));
    }

    @Operation(summary = "Mark one notification as read")
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id, Authentication authentication) {
        User user = getUser(authentication);
        notificationService.markRead(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Mark all notifications as read")
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllRead(Authentication authentication) {
        User user = getUser(authentication);
        notificationService.markAllRead(user.getId());
        return ResponseEntity.noContent().build();
    }

    private User getUser(Authentication authentication) {
        // Ensure a local profile exists for Keycloak-authenticated users.
        return currentUserResolver.resolveOrCreate(authentication);
    }
}
