package com.civicplatform.service.impl;

import com.civicplatform.dto.request.EventRequest;
import com.civicplatform.dto.response.EventParticipantResponse;
import com.civicplatform.dto.response.EventRegistrationStatusResponse;
import com.civicplatform.dto.response.EventResponse;
import com.civicplatform.entity.Event;
import com.civicplatform.entity.EventParticipant;
import com.civicplatform.entity.User;
import com.civicplatform.enums.EventStatus;
import com.civicplatform.enums.ParticipantStatus;
import com.civicplatform.exception.EventFullException;
import com.civicplatform.mapper.EventMapper;
import com.civicplatform.mapper.EventParticipantMapper;
import com.civicplatform.repository.EventParticipantRepository;
import com.civicplatform.repository.EventRepository;
import com.civicplatform.repository.UserRepository;
import com.civicplatform.service.EventService;
import com.civicplatform.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final EventMapper eventMapper;
    private final EventParticipantMapper eventParticipantMapper;
    private final PromotionService promotionService;

    @Override
    @Transactional
    public EventResponse createEvent(EventRequest eventRequest, Long organizerId) {
        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + organizerId));

        Event event = eventMapper.toEntity(eventRequest);
        event.setOrganizerId(organizerId);

        event = eventRepository.save(event);
        return eventMapper.toResponse(event);
    }

    @Override
    public EventResponse getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));
        return eventMapper.toResponse(event);
    }

    @Override
    public List<EventResponse> getAllEvents() {
        List<Event> events = eventRepository.findAll();
        return events.stream()
                .map(eventMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<EventResponse> getEventsByStatus(EventStatus status) {
        List<Event> events = eventRepository.findByStatus(status);
        return events.stream()
                .map(eventMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<EventResponse> getEventsByOrganizer(Long organizerId) {
        List<Event> events = eventRepository.findByOrganizerId(organizerId);
        return events.stream()
                .map(eventMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<EventResponse> getUpcomingEvents() {
        List<Event> events = eventRepository.findUpcomingEvents(LocalDateTime.now());
        return events.stream()
                .map(eventMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventResponse updateEvent(Long id, EventRequest eventRequest) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));

        eventMapper.updateEntity(eventRequest, event);
        event = eventRepository.save(event);
        return eventMapper.toResponse(event);
    }

    @Override
    @Transactional
    public void deleteEvent(Long id) {
        if (!eventRepository.existsById(id)) {
            throw new RuntimeException("Event not found with id: " + id);
        }
        eventRepository.deleteById(id);
    }

    @Override
    @Transactional
    public EventResponse cancelEvent(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));

        event.cancel();
        event = eventRepository.save(event);

        // Cancel all registrations
        List<EventParticipant> participants = eventParticipantRepository.findByEventIdOrderByRegisteredAtAsc(id);
        for (EventParticipant participant : participants) {
            participant.cancel();
        }
        eventParticipantRepository.saveAll(participants);

        return eventMapper.toResponse(event);
    }

    @Override
    @Transactional
    public void registerForEvent(Long eventId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));

        if (event.getStatus() != EventStatus.UPCOMING) {
            throw new RuntimeException("Can only register for upcoming events");
        }

        if (!event.hasCapacity()) {
            throw new EventFullException("Event is full");
        }

        EventParticipant existingParticipant = eventParticipantRepository.findByEventIdAndUserId(eventId, userId).orElse(null);
        if (existingParticipant != null) {
            if (existingParticipant.getStatus() == ParticipantStatus.CANCELLED) {
                existingParticipant.setStatus(ParticipantStatus.REGISTERED);
                existingParticipant.setRegisteredAt(LocalDateTime.now());
                existingParticipant.setCheckedInAt(null);
                existingParticipant.setCompletedAt(null);
                eventParticipantRepository.save(existingParticipant);
                event.incrementParticipants();
                eventRepository.save(event);
                return;
            }
            throw new RuntimeException("User is already registered for this event");
        }

        EventParticipant participant = EventParticipant.builder()
                .event(event)
                .user(user)
                .status(ParticipantStatus.REGISTERED)
                .build();

        eventParticipantRepository.save(participant);

        // Update event participant count
        event.incrementParticipants();
        eventRepository.save(event);
    }

    @Override
    @Transactional
    public void cancelRegistration(Long eventId, Long userId) {
        EventParticipant participant = eventParticipantRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new RuntimeException("User is not registered for this event"));

        participant.cancel();
        eventParticipantRepository.save(participant);

        // Update event participant count
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));
        event.decrementParticipants();
        eventRepository.save(event);
    }

    @Override
    @Transactional
    public void checkInParticipant(Long eventId, Long userId) {
        EventParticipant participant = eventParticipantRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new RuntimeException("User is not registered for this event"));

        if (participant.getStatus() != ParticipantStatus.REGISTERED) {
            throw new RuntimeException("Cannot check in user with status: " + participant.getStatus());
        }

        participant.checkIn();
        eventParticipantRepository.save(participant);
    }

    @Override
    @Transactional
    public void confirmAttendance(Long eventId, Long userId, boolean organizerConfirmation) {
        EventParticipant participant = eventParticipantRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new RuntimeException("User is not registered for this event"));

        if (organizerConfirmation) {
            if (participant.getStatus() != ParticipantStatus.CHECKED_IN) {
                throw new RuntimeException("User must be checked in before organizer confirms attendance");
            }
        } else if (participant.getStatus() == ParticipantStatus.REGISTERED) {
            participant.checkIn();
        } else if (participant.getStatus() != ParticipantStatus.CHECKED_IN) {
            throw new RuntimeException("Cannot confirm attendance with status: " + participant.getStatus());
        }

        participant.complete();
        eventParticipantRepository.save(participant);

        // Trigger promotion logic
        promotionService.processEventAttendance(userId);
    }

    @Override
    public List<EventParticipantResponse> getParticipationsByUser(Long userId) {
        List<EventParticipant> participations = eventParticipantRepository.findByUserIdWithEventAndUser(userId);
        return participations.stream()
                .map(eventParticipantMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public EventRegistrationStatusResponse getRegistrationStatus(Long eventId, Long userId) {
        return eventParticipantRepository.findByEventIdAndUserId(eventId, userId)
                .map(ep -> {
                    boolean active = ep.getStatus() != ParticipantStatus.CANCELLED;
                    return EventRegistrationStatusResponse.builder()
                            .registered(active)
                            .status(ep.getStatus().name())
                            .build();
                })
                .orElse(EventRegistrationStatusResponse.builder()
                        .registered(false)
                        .status(null)
                        .build());
    }
}
