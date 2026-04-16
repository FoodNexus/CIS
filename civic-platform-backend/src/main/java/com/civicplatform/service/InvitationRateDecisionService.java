package com.civicplatform.service;

import com.civicplatform.dto.rate.CitizenRateSnapshot;
import com.civicplatform.dto.rate.InvitationRateDecision;
import com.civicplatform.entity.Event;
import com.civicplatform.enums.EventStatus;
import com.civicplatform.enums.InvitationTier;
import com.civicplatform.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Advanced business logic: maps composite rate (+ context) to invitation tier and side-effects.
 * <ul>
 *   <li>High rate → priority immediate invite + follow-up flag + high-attention channel.</li>
 *   <li>Mid rate → standard invite.</li>
 *   <li>Low rate → no direct invite; nurture path with a suggested alternate event.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class InvitationRateDecisionService {

    private final EventRepository eventRepository;

    @Value("${app.invitation.rate.priority-threshold:72.0}")
    private double priorityThreshold;

    @Value("${app.invitation.rate.standard-threshold:45.0}")
    private double standardThreshold;

    public InvitationRateDecision decide(CitizenRateSnapshot snapshot, Event currentEvent) {
        double r = snapshot.getCompositeRate();

        if (r >= priorityThreshold) {
            return InvitationRateDecision.builder()
                    .tier(InvitationTier.PRIORITY_IMMEDIATE)
                    .priorityFollowup(true)
                    .engagementChannel("PRIORITY_MULTICHANNEL")
                    .nurtureSuggestedEventId(null)
                    .nurtureSuggestedEventTitle(null)
                    .build();
        }

        if (r >= standardThreshold) {
            return InvitationRateDecision.builder()
                    .tier(InvitationTier.STANDARD_INVITE)
                    .priorityFollowup(false)
                    .engagementChannel("STANDARD")
                    .nurtureSuggestedEventId(null)
                    .nurtureSuggestedEventTitle(null)
                    .build();
        }

        Event alternate = pickNurtureAlternate(currentEvent);
        return InvitationRateDecision.builder()
                .tier(InvitationTier.NURTURE_ALTERNATIVE)
                .priorityFollowup(false)
                .engagementChannel("NURTURE_SUGGEST_EVENT")
                .nurtureSuggestedEventId(alternate != null ? alternate.getId() : null)
                .nurtureSuggestedEventTitle(alternate != null ? alternate.getTitle() : null)
                .build();
    }

    private Event pickNurtureAlternate(Event current) {
        LocalDateTime now = LocalDateTime.now();
        List<Event> candidates = eventRepository.findUpcomingExcluding(now, current.getId());
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.stream()
                .filter(e -> e.getStatus() == EventStatus.UPCOMING)
                .filter(e -> e.getType() == current.getType())
                .findFirst()
                .orElseGet(() -> candidates.stream()
                        .filter(e -> e.getStatus() == EventStatus.UPCOMING)
                        .findFirst()
                        .orElse(null));
    }
}
