package com.civicplatform.service.impl;

import com.civicplatform.dto.request.EventRequest;
import com.civicplatform.dto.response.EventResponse;
import com.civicplatform.entity.Event;
import com.civicplatform.entity.User;
import com.civicplatform.enums.EventType;
import com.civicplatform.mapper.EventMapper;
import com.civicplatform.mapper.EventParticipantMapper;
import com.civicplatform.repository.EventParticipantRepository;
import com.civicplatform.repository.EventRepository;
import com.civicplatform.repository.UserRepository;
import com.civicplatform.service.EventInvitationMatchingAsyncRunner;
import com.civicplatform.service.EventLifecycleService;
import com.civicplatform.service.EventSearchService;
import com.civicplatform.service.UserInteractionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock private EventRepository eventRepository;
    @Mock private UserRepository userRepository;
    @Mock private EventParticipantRepository eventParticipantRepository;
    @Mock private EventMapper eventMapper;
    @Mock private EventParticipantMapper eventParticipantMapper;
    @Mock private EventLifecycleService eventLifecycleService;
    @Mock private EventInvitationMatchingAsyncRunner matchingAsyncRunner;
    @Mock private UserInteractionService userInteractionService;
    @Mock private EventSearchService eventSearchService;

    private EventServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EventServiceImpl(
                eventRepository,
                userRepository,
                eventParticipantRepository,
                eventMapper,
                eventParticipantMapper,
                eventLifecycleService,
                matchingAsyncRunner,
                userInteractionService,
                eventSearchService
        );
        ReflectionTestUtils.setField(service, "minLeadHours", 3);
    }

    @Test
    void createEvent_rejectsPastDateWithClearMessage() {
        EventRequest req = EventRequest.builder()
                .title("Bad date")
                .type(EventType.FORMATION)
                .maxCapacity(10)
                .date(LocalDateTime.now().minusHours(1).toString())
                .build();
        when(userRepository.findById(10L)).thenReturn(Optional.of(User.builder().id(10L).build()));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.createEvent(req, 10L));
        assertEquals("Event date must be at least 3 hour(s) in the future.", ex.getMessage());
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void createEvent_acceptsValidFutureDate() {
        EventRequest req = EventRequest.builder()
                .title("Good date")
                .type(EventType.FORMATION)
                .maxCapacity(10)
                .date(LocalDateTime.now().plusHours(4).toString())
                .build();
        Event mapped = Event.builder().title("Good date").build();
        Event saved = Event.builder().id(22L).title("Good date").build();
        EventResponse response = EventResponse.builder().id(22L).title("Good date").build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(User.builder().id(10L).build()));
        when(eventMapper.toEntity(req)).thenReturn(mapped);
        when(eventRepository.save(mapped)).thenReturn(saved);
        when(eventMapper.toResponse(saved)).thenReturn(response);

        EventResponse out = service.createEvent(req, 10L);

        assertEquals(22L, out.getId());
        verify(eventRepository).save(mapped);
    }
}
