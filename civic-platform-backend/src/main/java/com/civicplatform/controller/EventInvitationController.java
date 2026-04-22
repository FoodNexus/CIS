package com.civicplatform.controller;

import com.civicplatform.dto.response.EventInvitationResponse;
import com.civicplatform.entity.Event;
import com.civicplatform.entity.EventCitizenInvitation;
import com.civicplatform.entity.User;
import com.civicplatform.mapper.EventInvitationMapper;
import com.civicplatform.repository.EventRepository;
import com.civicplatform.security.CurrentUserResolver;
import com.civicplatform.service.EventInvitationMatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/event-invitations")
@RequiredArgsConstructor
public class EventInvitationController {

    private final EventInvitationMatchingService eventInvitationMatchingService;
    private final EventInvitationMapper eventInvitationMapper;
    private final EventRepository eventRepository;
    private final CurrentUserResolver currentUserResolver;

    @GetMapping("/events/{eventId}/invitations")
    public ResponseEntity<List<EventInvitationResponse>> getEventInvitations(
            @PathVariable Long eventId,
            Authentication authentication) {
        User requester = currentUserResolver.resolveOrCreate(authentication);
        List<EventCitizenInvitation> matches =
                eventInvitationMatchingService.getEventInvitations(eventId, requester.getId());
        return ResponseEntity.ok(
                matches.stream().map(eventInvitationMapper::toResponseForOrganizer).toList()
        );
    }

    @GetMapping("/my-invitations")
    public ResponseEntity<List<EventInvitationResponse>> getMyInvitations(
            Authentication authentication) {
        User user = currentUserResolver.resolveOrCreate(authentication);
        return ResponseEntity.ok(
                eventInvitationMatchingService.getMyInvitations(user.getId())
                        .stream().map(eventInvitationMapper::toResponse).toList()
        );
    }

    @GetMapping("/respond")
    public ResponseEntity<Map<String, String>> respond(
            @RequestParam String token,
            @RequestParam String response) {
        if (!"ACCEPTED".equals(response) && !"DECLINED".equals(response)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid response value"));
        }
        EventCitizenInvitation invitation =
                eventInvitationMatchingService.respondToInvitation(token, response);
        String message = "ACCEPTED".equals(response)
                ? "Thank you! Your response has been recorded."
                : "Your response has been recorded. Thank you.";
        return ResponseEntity.ok(Map.of(
                "status", response,
                "message", message,
                "eventTitle", invitation.getEvent().getTitle()
        ));
    }

    @PostMapping("/events/{eventId}/rematch")
    public ResponseEntity<List<EventInvitationResponse>> triggerMatching(
            @PathVariable Long eventId,
            Authentication authentication) {
        User requester = currentUserResolver.resolveOrCreate(authentication);
        if (!requester.isAdmin()) {
            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Event not found"));
            if (event.getOrganizerId() == null || !event.getOrganizerId().equals(requester.getId())) {
                return ResponseEntity.status(403).build();
            }
        }
        List<EventCitizenInvitation> matches =
                eventInvitationMatchingService.forceMatchCitizensForEvent(eventId);
        return ResponseEntity.ok(
                matches.stream().map(eventInvitationMapper::toResponseForOrganizer).toList()
        );
    }
}
