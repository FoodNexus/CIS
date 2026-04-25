package com.civicplatform.controller;

import com.civicplatform.dto.request.EventRequest;
import com.civicplatform.dto.request.EventStatusUpdateRequest;
import com.civicplatform.dto.response.EventParticipantResponse;
import com.civicplatform.dto.response.EventRegistrationStatusResponse;
import com.civicplatform.dto.response.EventResponse;
import com.civicplatform.entity.User;
import com.civicplatform.enums.EventStatus;
import com.civicplatform.enums.UserType;
import com.civicplatform.security.CurrentUserResolver;
import com.civicplatform.security.RegularAccountPolicy;
import com.civicplatform.service.CertificateService;
import com.civicplatform.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/events", "/v1/events"})
@RequiredArgsConstructor
@Tag(name = "Event Management", description = "Event management APIs")
@Validated
public class EventController {

    private final EventService eventService;
    private final CertificateService certificateService;
    private final CurrentUserResolver currentUserResolver;

    @Operation(summary = "Create a new event")
    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody EventRequest eventRequest, Authentication authentication) {
        User user = getUserFromAuthentication(authentication);
        if (user.isAdmin()) {
            EventResponse response = eventService.createEvent(eventRequest, user.getId());
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        }
        RegularAccountPolicy.requireRegularUser(user);
        if (user.getUserType() != UserType.DONOR && user.getUserType() != UserType.AMBASSADOR) {
            throw new AccessDeniedException("Only DONOR and AMBASSADOR users can create events");
        }
        Long userId = user.getId();
        EventResponse response = eventService.createEvent(eventRequest, userId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Get my event participations")
    @GetMapping("/my-participations")
    public ResponseEntity<List<EventParticipantResponse>> getMyParticipations(Authentication authentication) {
        User user = getUserFromAuthentication(authentication);
        RegularAccountPolicy.requireRegularUser(user);
        List<EventParticipantResponse> response = eventService.getParticipationsByUser(user.getId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get upcoming events")
    @GetMapping("/upcoming")
    public ResponseEntity<List<EventResponse>> getUpcomingEvents() {
        List<EventResponse> response = eventService.getUpcomingEvents();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get events by status")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<EventResponse>> getEventsByStatus(@PathVariable EventStatus status) {
        List<EventResponse> response = eventService.getEventsByStatus(status);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get events by organizer")
    @GetMapping("/organizer/{organizerId}")
    public ResponseEntity<List<EventResponse>> getEventsByOrganizer(@PathVariable Long organizerId) {
        List<EventResponse> response = eventService.getEventsByOrganizer(organizerId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all events")
    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllEvents() {
        List<EventResponse> response = eventService.getAllEvents();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get event feed with deterministic sorting")
    @GetMapping("/feed")
    public ResponseEntity<List<EventResponse>> getEventsFeed(
            @RequestParam(name = "sort", defaultValue = "recent") String sort) {
        if ("popularity".equalsIgnoreCase(sort)) {
            return ResponseEntity.ok(eventService.getFeedByPopularity());
        }
        return ResponseEntity.ok(eventService.getAllEvents());
    }

    /**
     * Paginated event search with constrained JQL-like filtering.
     * Example: {@code status = UPCOMING AND type = FORMATION AND title ~ 'food'}.
     */
    @Operation(summary = "Search events with pagination and JQL-like filters")
    @GetMapping("/search")
    public ResponseEntity<Page<EventResponse>> searchEvents(
            @RequestParam(required = false) String jql,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<EventResponse> response = eventService.searchEvents(jql, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Whether the current user is registered for this event")
    @GetMapping("/{id}/registration")
    public ResponseEntity<EventRegistrationStatusResponse> getRegistrationStatus(
            @PathVariable Long id,
            Authentication authentication) {
        User user = getUserFromAuthentication(authentication);
        RegularAccountPolicy.requireRegularUser(user);
        EventRegistrationStatusResponse response = eventService.getRegistrationStatus(id, user.getId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Download PDF certificate of participation (completed attendance only)")
    @GetMapping("/{eventId}/attendance/{userId}/certificate/pdf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> getParticipationCertificate(
            @PathVariable Long eventId,
            @PathVariable Long userId,
            Authentication authentication) {
        User authUser = getUserFromAuthentication(authentication);
        RegularAccountPolicy.requireRegularUser(authUser);
        if (!authUser.getId().equals(userId)) {
            throw new AccessDeniedException("You can only download your own certificate");
        }
        byte[] pdf = certificateService.generateCertificate(eventId, userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"certificate-event-"
                                + eventId + "-user-" + userId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @Operation(summary = "Transition event status (triggers user-type lifecycle)")
    @PatchMapping("/{id}/status")
    public ResponseEntity<EventResponse> transitionEventStatus(
            @PathVariable Long id,
            @Valid @RequestBody EventStatusUpdateRequest body,
            Authentication authentication) {
        checkEventOwnership(id, authentication);
        EventResponse response = eventService.transitionEventStatus(id, body.getStatus());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get event by ID")
    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable Long id) {
        EventResponse response = eventService.getEventById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update event")
    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> updateEvent(@PathVariable Long id, @Valid @RequestBody EventRequest eventRequest, Authentication authentication) {
        checkEventOwnership(id, authentication);
        EventResponse response = eventService.updateEvent(id, eventRequest);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete event")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id, Authentication authentication) {
        checkEventOwnership(id, authentication);
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Cancel event")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<EventResponse> cancelEvent(@PathVariable Long id, Authentication authentication) {
        checkEventOwnership(id, authentication);
        EventResponse response = eventService.cancelEvent(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Register for event")
    @PostMapping("/{id}/register")
    public ResponseEntity<Void> registerForEvent(@PathVariable Long id, Authentication authentication) {
        User user = getUserFromAuthentication(authentication);
        RegularAccountPolicy.requireRegularUser(user);
        eventService.registerForEvent(id, user.getId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Cancel registration")
    @DeleteMapping("/{id}/register")
    public ResponseEntity<Void> cancelRegistration(@PathVariable Long id, Authentication authentication) {
        User user = getUserFromAuthentication(authentication);
        RegularAccountPolicy.requireRegularUser(user);
        eventService.cancelRegistration(id, user.getId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Check in participant")
    @PostMapping("/{id}/checkin")
    public ResponseEntity<Void> checkInParticipant(@PathVariable Long id, @RequestParam Long userId, Authentication authentication) {
        checkEventOwnership(id, authentication);
        eventService.checkInParticipant(id, userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Confirm attendance and trigger promotion")
    @PostMapping("/{id}/attend")
    public ResponseEntity<Void> confirmAttendance(@PathVariable Long id, Authentication authentication) {
        User user = getUserFromAuthentication(authentication);
        RegularAccountPolicy.requireRegularUser(user);
        eventService.confirmAttendance(id, user.getId(), false);
        return ResponseEntity.ok().build();
    }

    private void checkEventOwnership(Long eventId, Authentication authentication) {
        User user = getUserFromAuthentication(authentication);
        if (user.isAdmin()) {
            return;
        }
        RegularAccountPolicy.requireRegularUser(user);
        EventResponse event = eventService.getEventById(eventId);
        if (!user.getId().equals(event.getOrganizerId())) {
            throw new AccessDeniedException("You are not the organizer of this event");
        }
    }

    private User getUserFromAuthentication(Authentication authentication) {
        return currentUserResolver.resolveOrCreate(authentication);
    }
}
