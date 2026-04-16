package com.civicplatform.service;

import com.civicplatform.dto.rate.CitizenRateSnapshot;
import com.civicplatform.dto.rate.InvitationRateDecision;
import com.civicplatform.entity.Event;
import com.civicplatform.entity.EventCitizenInvitation;
import com.civicplatform.entity.User;
import com.civicplatform.enums.InvitationTier;
import com.civicplatform.enums.MatchStatus;
import com.civicplatform.enums.NotificationType;
import com.civicplatform.enums.UserType;
import com.civicplatform.repository.EventCitizenInvitationRepository;
import com.civicplatform.repository.EventRepository;
import com.civicplatform.repository.UserRepository;
import com.civicplatform.util.FoodCommunityContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventInvitationMatchingService {

    private record InvitationScoreRow(User citizen, CitizenRateSnapshot snapshot, InvitationRateDecision decision) {}

    private static final int MAX_DIRECT_INVITES = 10;
    private static final int MAX_NURTURE_NOTIFICATIONS = 8;
    /** Only consider nurture path for citizens in the top ranks by composite rate. */
    private static final int NURTURE_RANK_CUTOFF = 15;

    private final EventRepository eventRepository;
    private final EventCitizenInvitationRepository eventCitizenInvitationRepository;
    private final CitizenRateCalculationService citizenRateCalculationService;
    private final InvitationRateDecisionService invitationRateDecisionService;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.public-base-url:http://localhost:8082/api}")
    private String publicBaseUrl;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${app.invitation.food.min-composite:70.0}")
    private double foodMinComposite;

    @Value("${app.invitation.food.min-direct-invites:5}")
    private int foodMinDirectInvites;

    @Value("${app.invitation.food.sms-followup-threshold:85.0}")
    private double foodSmsFollowupThreshold;

    @Value("${app.invitation.rate.standard-threshold:45.0}")
    private double standardThreshold;

    @Transactional
    public List<EventCitizenInvitation> matchCitizensForEventIfAbsent(Long eventId) {
        if (eventCitizenInvitationRepository.existsByEvent_Id(eventId)) {
            return eventCitizenInvitationRepository.findByEventIdOrderByScoreDesc(eventId);
        }
        return runMatching(eventId);
    }

    @Transactional
    public List<EventCitizenInvitation> forceMatchCitizensForEvent(Long eventId) {
        eventCitizenInvitationRepository.deleteByEventId(eventId);
        eventCitizenInvitationRepository.flush();
        return runMatching(eventId);
    }

    private List<EventCitizenInvitation> runMatching(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + eventId));
        Long organizerId = event.getOrganizerId();
        if (organizerId == null) {
            log.warn("Citizen invitation matching skipped: event {} has no organizer", eventId);
            return List.of();
        }

        User donor = userRepository.findById(organizerId)
                .orElseThrow(() -> new EntityNotFoundException("Organizer not found: " + organizerId));

        String baseFrontend = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;

        List<InvitationScoreRow> ranked = new ArrayList<>();
        for (User citizen : userRepository.findEligibleCitizensForInvitations()) {
            if (citizen.getUserType() != UserType.AMBASSADOR
                    && citizen.getUserType() != UserType.PARTICIPANT
                    && citizen.getUserType() != UserType.CITIZEN) {
                continue;
            }
            if (citizen.isAdmin()) {
                continue;
            }
            if (citizen.getId().equals(organizerId)) {
                continue;
            }

            CitizenRateSnapshot snapshot = citizenRateCalculationService.compute(citizen, event, organizerId);
            InvitationRateDecision decision = invitationRateDecisionService.decide(snapshot, event);
            ranked.add(new InvitationScoreRow(citizen, snapshot, decision));
        }

        ranked.sort(Comparator.comparing((InvitationScoreRow s) -> s.snapshot().getCompositeRate()).reversed());

        int nurtureSent = 0;
        for (int i = 0; i < Math.min(ranked.size(), NURTURE_RANK_CUTOFF) && nurtureSent < MAX_NURTURE_NOTIFICATIONS; i++) {
            InvitationScoreRow row = ranked.get(i);
            if (row.decision().getTier() != InvitationTier.NURTURE_ALTERNATIVE) {
                continue;
            }
            sendNurturePath(row.citizen(), event, donor, row.decision(), baseFrontend);
            nurtureSent++;
        }

        List<InvitationScoreRow> toInvite = selectDirectInvitees(ranked, event);
        List<EventCitizenInvitation> saved = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (InvitationScoreRow row : toInvite) {
            User citizen = row.citizen();
            CitizenRateSnapshot snapshot = row.snapshot();
            InvitationRateDecision decision = row.decision();

            if (eventCitizenInvitationRepository.existsByEvent_IdAndCitizen_Id(eventId, citizen.getId())) {
                continue;
            }

            String token = UUID.randomUUID().toString();
            String featureJson = toFeatureJson(snapshot);

            EventCitizenInvitation invitation = EventCitizenInvitation.builder()
                    .event(event)
                    .citizen(citizen)
                    .matchScore(snapshot.getRawTotal())
                    .compositeRate(snapshot.getCompositeRate())
                    .invitationTier(decision.getTier())
                    .priorityFollowup(decision.isPriorityFollowup())
                    .featureBreakdownJson(featureJson)
                    .status(MatchStatus.INVITED)
                    .invitedAt(now)
                    .invitationToken(token)
                    .build();
            invitation = eventCitizenInvitationRepository.save(invitation);
            saved.add(invitation);

            try {
                emailService.sendCitizenEventInvitation(citizen, event, donor, token, snapshot.getCompositeRate(), publicBaseUrl);
            } catch (Exception ex) {
                log.warn("Citizen invitation email failed for event {} citizen {}: {}",
                        eventId, citizen.getId(), ex.getMessage());
            }

            String body = buildInviteNotificationBody(event.getTitle(), decision);
            notificationService.notifyUnlessSameUser(
                    citizen.getId(),
                    donor.getId(),
                    NotificationType.ENGAGEMENT,
                    "Event invitation",
                    body,
                    baseFrontend + "/dashboard?tab=invitations");

            if (FoodCommunityContext.matchesEvent(event)
                    && snapshot.getCompositeRate() >= foodSmsFollowupThreshold) {
                notificationService.notifyUnlessSameUser(
                        citizen.getId(),
                        donor.getId(),
                        NotificationType.ENGAGEMENT,
                        "High-match invitation (SMS follow-up)",
                        "You are a top match for \"" + event.getTitle()
                                + "\". If your phone number is on file, you may receive a reminder SMS.",
                        baseFrontend + "/dashboard?tab=invitations");
            }
        }

        return saved;
    }

    /**
     * Food/community events: prefer citizens at or above {@link #foodMinComposite}; if fewer than
     * {@link #foodMinDirectInvites} qualify, supplement with the next best citizens above the global
     * standard threshold (still excluding nurture-tier rows).
     */
    private List<InvitationScoreRow> selectDirectInvitees(List<InvitationScoreRow> ranked, Event event) {
        List<InvitationScoreRow> nonNurture = ranked.stream()
                .filter(s -> s.decision().getTier() != InvitationTier.NURTURE_ALTERNATIVE)
                .toList();
        if (!FoodCommunityContext.matchesEvent(event)) {
            return nonNurture.stream().limit(MAX_DIRECT_INVITES).toList();
        }
        List<InvitationScoreRow> atOrAboveFoodMin = nonNurture.stream()
                .filter(s -> s.snapshot().getCompositeRate() >= foodMinComposite)
                .toList();
        List<InvitationScoreRow> primary = atOrAboveFoodMin.stream().limit(MAX_DIRECT_INVITES).toList();
        if (primary.size() >= foodMinDirectInvites || primary.size() >= MAX_DIRECT_INVITES) {
            return primary;
        }
        Set<Long> seen = new HashSet<>(primary.stream().map(s -> s.citizen().getId()).toList());
        List<InvitationScoreRow> result = new ArrayList<>(primary);
        for (InvitationScoreRow s : nonNurture) {
            if (result.size() >= MAX_DIRECT_INVITES) {
                break;
            }
            if (seen.contains(s.citizen().getId())) {
                continue;
            }
            if (s.snapshot().getCompositeRate() >= standardThreshold) {
                result.add(s);
                seen.add(s.citizen().getId());
            }
        }
        return result;
    }

    private String buildInviteNotificationBody(String eventTitle, InvitationRateDecision decision) {
        String base = "You were invited to \"" + eventTitle + "\". Open My invitations to accept or decline.";
        if (decision.getTier() == InvitationTier.PRIORITY_IMMEDIATE) {
            return base + " You are on the priority list for follow-up from the organizer.";
        }
        return base;
    }

    private void sendNurturePath(User citizen, Event event, User donor, InvitationRateDecision decision, String baseFrontend) {
        Long altId = decision.getNurtureSuggestedEventId();
        String altTitle = decision.getNurtureSuggestedEventTitle();
        String link = altId != null
                ? baseFrontend + "/events/" + altId
                : baseFrontend + "/events";
        String detail = altTitle != null
                ? "We prioritized other community members for \"" + event.getTitle() + "\". You may enjoy this related event: \"" + altTitle + "\"."
                : "We prioritized other community members for \"" + event.getTitle() + "\". Browse upcoming events to find a better fit.";
        notificationService.notifyUnlessSameUser(
                citizen.getId(),
                donor.getId(),
                NotificationType.ENGAGEMENT,
                "Suggested event for you",
                detail,
                link);
        log.info("Nurture path for user {} (event {}): suggestedEventId={}", citizen.getId(), event.getId(), altId);
    }

    private String toFeatureJson(CitizenRateSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot.getFeatures());
        } catch (Exception e) {
            return "{}";
        }
    }

    @Transactional
    public EventCitizenInvitation respondToInvitation(String token, String response) {
        EventCitizenInvitation invitation = eventCitizenInvitationRepository.findByInvitationToken(token)
                .orElseThrow(() -> new EntityNotFoundException("Invitation not found"));

        if (invitation.getStatus() != MatchStatus.INVITED) {
            throw new IllegalStateException("Already responded");
        }

        MatchStatus newStatus = "ACCEPTED".equalsIgnoreCase(response) ? MatchStatus.ACCEPTED : MatchStatus.DECLINED;
        invitation.setStatus(newStatus);
        invitation.setRespondedAt(LocalDateTime.now());
        invitation = eventCitizenInvitationRepository.save(invitation);

        if (newStatus == MatchStatus.ACCEPTED && invitation.getEvent().getOrganizerId() != null) {
            User donor = userRepository.findById(invitation.getEvent().getOrganizerId()).orElse(null);
            User citizen = invitation.getCitizen();
            if (donor != null) {
                try {
                    double scoreForEmail = invitation.getCompositeRate() != null
                            ? invitation.getCompositeRate()
                            : invitation.getMatchScore();
                    emailService.sendCitizenAcceptedNotification(
                            donor,
                            citizen,
                            invitation.getEvent(),
                            scoreForEmail,
                            publicBaseUrl);
                } catch (Exception ex) {
                    log.warn("Donor acceptance email failed: {}", ex.getMessage());
                }
            }
        }

        return invitation;
    }

    @Transactional(readOnly = true)
    public List<EventCitizenInvitation> getEventInvitations(Long eventId, Long requesterId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + eventId));
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + requesterId));

        boolean allowed = requester.isAdmin()
                || (event.getOrganizerId() != null && event.getOrganizerId().equals(requesterId));
        if (!allowed) {
            throw new org.springframework.security.access.AccessDeniedException("Not allowed to view event invitations");
        }

        return eventCitizenInvitationRepository.findByEventIdOrderByScoreDesc(eventId);
    }

    @Transactional(readOnly = true)
    public List<EventCitizenInvitation> getMyInvitations(Long userId) {
        return eventCitizenInvitationRepository.findByCitizenIdOrderByInvitedAtDesc(userId);
    }
}
