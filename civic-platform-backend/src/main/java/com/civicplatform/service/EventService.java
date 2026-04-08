package com.civicplatform.service;

import com.civicplatform.dto.request.EventRequest;
import com.civicplatform.dto.response.EventParticipantResponse;
import com.civicplatform.dto.response.EventRegistrationStatusResponse;
import com.civicplatform.dto.response.EventResponse;
import com.civicplatform.enums.EventStatus;

import java.util.List;

public interface EventService {
    EventResponse createEvent(EventRequest eventRequest, Long organizerId);
    EventResponse getEventById(Long id);
    List<EventResponse> getAllEvents();
    List<EventResponse> getEventsByStatus(EventStatus status);
    List<EventResponse> getEventsByOrganizer(Long organizerId);
    List<EventResponse> getUpcomingEvents();
    EventResponse updateEvent(Long id, EventRequest eventRequest);
    void deleteEvent(Long id);
    EventResponse cancelEvent(Long id);
    void registerForEvent(Long eventId, Long userId);
    void cancelRegistration(Long eventId, Long userId);
    void checkInParticipant(Long eventId, Long userId);
    void confirmAttendance(Long eventId, Long userId, boolean organizerConfirmation);
    List<EventParticipantResponse> getParticipationsByUser(Long userId);
    EventRegistrationStatusResponse getRegistrationStatus(Long eventId, Long userId);
}
