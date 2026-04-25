package com.civicplatform.service;

import com.civicplatform.dto.request.EventRequest;
import com.civicplatform.dto.response.EventParticipantResponse;
import com.civicplatform.dto.response.EventRegistrationStatusResponse;
import com.civicplatform.dto.response.EventResponse;
import com.civicplatform.enums.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface EventService {
    EventResponse createEvent(EventRequest eventRequest, Long organizerId);
    EventResponse getEventById(Long id);
    List<EventResponse> getAllEvents();
    List<EventResponse> getFeedByPopularity();
    List<EventResponse> getEventsByStatus(EventStatus status);
    List<EventResponse> getEventsByOrganizer(Long organizerId);
    List<EventResponse> getUpcomingEvents();
    Page<EventResponse> searchEvents(String jql, Pageable pageable);
    EventResponse updateEvent(Long id, EventRequest eventRequest);

    EventResponse transitionEventStatus(Long id, EventStatus newStatus);
    void deleteEvent(Long id);
    EventResponse cancelEvent(Long id);
    void registerForEvent(Long eventId, Long userId);
    void cancelRegistration(Long eventId, Long userId);
    void checkInParticipant(Long eventId, Long userId);
    void confirmAttendance(Long eventId, Long userId, boolean organizerConfirmation);
    List<EventParticipantResponse> getParticipationsByUser(Long userId);
    EventRegistrationStatusResponse getRegistrationStatus(Long eventId, Long userId);
}
