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
import com.civicplatform.enums.InteractionAction;
import com.civicplatform.enums.InteractionEntityType;
import com.civicplatform.service.EventInvitationMatchingAsyncRunner;
import com.civicplatform.service.EventLifecycleService;
import com.civicplatform.service.EventSearchService;
import com.civicplatform.service.EventService;
import com.civicplatform.service.ScoringProperties;
import com.civicplatform.service.UserInteractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final EventMapper eventMapper;
    private final EventParticipantMapper eventParticipantMapper;
    private final EventLifecycleService eventLifecycleService;
    private final EventInvitationMatchingAsyncRunner eventInvitationMatchingAsyncRunner;
    private final UserInteractionService userInteractionService;
    private final EventSearchService eventSearchService;
    private final ScoringProperties scoringProperties;

    @Value("${app.events.min-lead-hours:3}")
    private int minLeadHours;

    @Override
    @Transactional
    public EventResponse createEvent(EventRequest eventRequest, Long organizerId) {
        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + organizerId));
        validateEventDate(eventRequest.getDate());

        Event event = eventMapper.toEntity(eventRequest);
        event.setOrganizerId(organizerId);

        event = eventRepository.save(event);
        final Long newEventId = event.getId();
        // Run matching only after this transaction commits so the async task can load the event from the DB.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eventInvitationMatchingAsyncRunner.triggerMatchingAfterEventCreated(newEventId);
                }
            });
        } else {
            eventInvitationMatchingAsyncRunner.triggerMatchingAfterEventCreated(newEventId);
        }
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

    /**
     * Returns events sorted by deterministic popularity score.
     */
    @Override
    public List<EventResponse> getFeedByPopularity() {
        ScoringProperties.EventPopularity cfg = scoringProperties.getEventPopularity();
        List<Event> events = eventRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        record EventWithScore(Event event, double score) {}
        List<EventWithScore> scored = events.stream()
                .map(event -> new EventWithScore(event, calculatePopularityScore(event, now, cfg)))
                .toList();

        return scored.stream()
                .sorted((left, right) -> {
                    int byScore = Double.compare(right.score(), left.score());
                    if (byScore != 0) {
                        return byScore;
                    }
                    Event leftEvent = left.event();
                    Event rightEvent = right.event();
                    LocalDateTime leftDate = leftEvent.getDate() == null ? LocalDateTime.MAX : leftEvent.getDate();
                    LocalDateTime rightDate = rightEvent.getDate() == null ? LocalDateTime.MAX : rightEvent.getDate();
                    int byStartDate = leftDate.compareTo(rightDate);
                    if (byStartDate != 0) {
                        return byStartDate;
                    }
                    LocalDateTime rightCreatedAt = rightEvent.getCreatedAt() == null ? LocalDateTime.MIN : rightEvent.getCreatedAt();
                    LocalDateTime leftCreatedAt = leftEvent.getCreatedAt() == null ? LocalDateTime.MIN : leftEvent.getCreatedAt();
                    return rightCreatedAt.compareTo(leftCreatedAt);
                })
                .map(item -> {
                    EventResponse response = eventMapper.toResponse(item.event());
                    response.setPopularityScore(item.score());
                    return response;
                })
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
    public Page<EventResponse> searchEvents(String jql, Pageable pageable) {
        return eventSearchService.search(jql, pageable).map(eventMapper::toResponse);
    }

    @Override
    @Transactional
    public EventResponse updateEvent(Long id, EventRequest eventRequest) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));
        validateEventDate(eventRequest.getDate());

        eventMapper.updateEntity(eventRequest, event);
        event = eventRepository.save(event);
        return eventMapper.toResponse(event);
    }

    @Override
    @Transactional
    public EventResponse transitionEventStatus(Long id, EventStatus newStatus) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));
        EventStatus oldStatus = event.getStatus();
        if (oldStatus == newStatus) {
            return eventMapper.toResponse(event);
        }
        event.setStatus(newStatus);
        event = eventRepository.save(event);

        if (newStatus == EventStatus.COMPLETED) {
            finalizeParticipationsForCompletedEvent(id);
        }

        handleEventStatusTransition(event.getId(), oldStatus, event.getStatus());

        if (newStatus == EventStatus.CANCELLED) {
            List<EventParticipant> participants = eventParticipantRepository.findByEventIdOrderByRegisteredAtAsc(id);
            for (EventParticipant participant : participants) {
                participant.cancel();
            }
            eventParticipantRepository.saveAll(participants);
        }

        return eventMapper.toResponse(event);
    }

    /**
     * When an event is marked COMPLETED, mark active participations as COMPLETED so attendance counts
     * and lifecycle (user_type) see this event as a finished attendance.
     */
    private void finalizeParticipationsForCompletedEvent(Long eventId) {
        List<EventParticipant> participants = eventParticipantRepository.findByEventIdOrderByRegisteredAtAsc(eventId);
        for (EventParticipant ep : participants) {
            if (ep.getStatus() == ParticipantStatus.REGISTERED || ep.getStatus() == ParticipantStatus.CHECKED_IN) {
                ep.complete();
            }
        }
        eventParticipantRepository.saveAll(participants);
    }

    private void handleEventStatusTransition(Long eventId, EventStatus oldStatus, EventStatus newStatus) {
        if (oldStatus == newStatus) {
            return;
        }
        if (newStatus == EventStatus.ONGOING) {
            eventLifecycleService.onEventStarted(eventId);
        }
        if (newStatus == EventStatus.COMPLETED || newStatus == EventStatus.CANCELLED) {
            if (oldStatus != EventStatus.COMPLETED && oldStatus != EventStatus.CANCELLED) {
                eventLifecycleService.onEventCompleted(eventId);
            }
        }
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
        return transitionEventStatus(id, EventStatus.CANCELLED);
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

        if (Objects.equals(event.getOrganizerId(), userId)) {
            throw new RuntimeException("Event organizers cannot register as participants for their own event");
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

        userInteractionService.record(userId, InteractionEntityType.EVENT, eventId, InteractionAction.ATTEND);
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
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));
        EventParticipant participant = eventParticipantRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new RuntimeException("User is not registered for this event"));

        if (organizerConfirmation) {
            if (participant.getStatus() != ParticipantStatus.CHECKED_IN) {
                throw new RuntimeException("User must be checked in before organizer confirms attendance");
            }
        } else if (participant.getStatus() == ParticipantStatus.REGISTERED
                && event.getStatus() != EventStatus.COMPLETED) {
            participant.checkIn();
        } else if (participant.getStatus() != ParticipantStatus.CHECKED_IN) {
            throw new RuntimeException("Cannot confirm attendance with status: " + participant.getStatus());
        }

        // Keep participant state active until event lifecycle closes the event.
        if (event.getStatus() == EventStatus.COMPLETED) {
            participant.complete();
        } else {
            participant.setStatus(ParticipantStatus.CHECKED_IN);
        }
        eventParticipantRepository.save(participant);
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

    private void validateEventDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            throw new IllegalStateException("Event date is required.");
        }
        final LocalDateTime parsed;
        try {
            parsed = LocalDateTime.parse(rawDate);
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid event date format. Please use a valid date and time.");
        }
        LocalDateTime minAllowed = LocalDateTime.now().plusHours(Math.max(1, minLeadHours));
        if (parsed.isBefore(minAllowed)) {
            throw new IllegalStateException(
                    "Event date must be at least " + Math.max(1, minLeadHours) + " hour(s) in the future."
            );
        }
    }

    private double calculatePopularityScore(Event event, LocalDateTime now, ScoringProperties.EventPopularity cfg) {
        long currentParticipants = event.getCurrentParticipants() == null ? 0L : event.getCurrentParticipants();
        long maxParticipants = event.getMaxCapacity() == null ? 0L : event.getMaxCapacity();
        double inscriptionRate = (double) currentParticipants / Math.max(1.0d, maxParticipants);
        double organizerReliability = calculateOrganizerReliability(event.getOrganizerId(), now, cfg.getHistoryDays());
        long daysUntilEvent = event.getDate() == null ? Long.MAX_VALUE
                : Math.max(0L, java.time.Duration.between(now, event.getDate()).toDays());
        double urgencyBoost = 1.0d / (daysUntilEvent + 1.0d);

        double score = (inscriptionRate * cfg.getWeightInscriptionRate())
                + (organizerReliability * cfg.getWeightOrganizerReliability())
                + (urgencyBoost * cfg.getWeightUrgency());
        log.debug("event={} inscriptionRate={} organizerReliability={} urgencyBoost={} score={}",
                event.getId(), inscriptionRate, organizerReliability, urgencyBoost, score);
        return score;
    }

    private double calculateOrganizerReliability(Long organizerId, LocalDateTime now, int historyDays) {
        if (organizerId == null) {
            return 0.0d;
        }
        LocalDateTime from = now.minusDays(Math.max(1, historyDays));
        List<Event> history = eventRepository.findByOrganizerIdAndDateBetween(organizerId, from, now);
        if (history.isEmpty()) {
            return 0.0d;
        }
        double sumRates = 0.0d;
        for (Event e : history) {
            long participants = e.getCurrentParticipants() == null ? 0L : e.getCurrentParticipants();
            long capacity = e.getMaxCapacity() == null ? 0L : e.getMaxCapacity();
            sumRates += ((double) participants / Math.max(1.0d, capacity));
        }
        return sumRates / history.size();
    }
}
