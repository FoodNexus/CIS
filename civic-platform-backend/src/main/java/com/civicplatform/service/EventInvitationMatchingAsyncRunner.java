package com.civicplatform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventInvitationMatchingAsyncRunner {

    private final EventInvitationMatchingService eventInvitationMatchingService;

    @Async
    public void triggerMatchingAfterEventCreated(Long eventId) {
        try {
            eventInvitationMatchingService.matchCitizensForEventIfAbsent(eventId);
        } catch (Exception e) {
            log.warn("Event invitation matching failed for event {}: {}", eventId, e.getMessage());
        }
    }
}
