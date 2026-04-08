package com.civicplatform.service.impl;

import com.civicplatform.entity.Event;
import com.civicplatform.entity.EventParticipant;
import com.civicplatform.entity.User;
import com.civicplatform.enums.EventStatus;
import com.civicplatform.enums.ParticipantStatus;
import com.civicplatform.enums.UserType;
import com.civicplatform.repository.EventParticipantRepository;
import com.civicplatform.repository.EventRepository;
import com.civicplatform.repository.UserRepository;
import com.civicplatform.service.BadgeService;
import com.civicplatform.service.EmailService;
import com.civicplatform.service.EventLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventLifecycleServiceImpl implements EventLifecycleService {

    private final EventRepository eventRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final UserRepository userRepository;
    private final BadgeService badgeService;
    private final EmailService emailService;

    @Override
    @Transactional
    public void onEventStarted(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("Event not found: " + eventId));
        if (event.getStatus() != EventStatus.ONGOING) {
            return;
        }

        List<EventParticipant> rows = eventParticipantRepository.findByEventIdOrderByRegisteredAtAsc(eventId);
        for (EventParticipant ep : rows) {
            if (ep.getStatus() != ParticipantStatus.REGISTERED && ep.getStatus() != ParticipantStatus.CHECKED_IN) {
                continue;
            }
            User user = userRepository.findById(ep.getUser().getId()).orElseThrow();
            if (user.getUserType() == UserType.DONOR || user.getUserType() == UserType.AMBASSADOR) {
                continue;
            }

            long attended = badgeService.countAttendedCompletedEvents(user.getId());
            if (attended >= 5) {
                if (user.getUserType() != UserType.AMBASSADOR) {
                    user.setUserType(UserType.AMBASSADOR);
                    userRepository.save(user);
                    emailService.sendAmbassadorPromotionEmail(user.getEmail(), user.getUserName());
                    log.info("User {} promoted to AMBASSADOR on event start (>=5 completed events)", user.getEmail());
                }
            } else if (user.getUserType() == UserType.CITIZEN) {
                user.setUserType(UserType.PARTICIPANT);
                userRepository.save(user);
            }
        }
    }

    @Override
    @Transactional
    public void onEventCompleted(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("Event not found: " + eventId));
        if (event.getStatus() != EventStatus.COMPLETED && event.getStatus() != EventStatus.CANCELLED) {
            return;
        }

        List<EventParticipant> rows = eventParticipantRepository.findByEventIdOrderByRegisteredAtAsc(eventId);
        Set<Long> seen = new HashSet<>();
        for (EventParticipant ep : rows) {
            Long uid = ep.getUser().getId();
            if (!seen.add(uid)) {
                continue;
            }
            User user = userRepository.findById(uid).orElseThrow();

            if (user.getUserType() == UserType.DONOR) {
                badgeService.applyBadgeForUser(user);
                userRepository.save(user);
                continue;
            }
            if (user.getUserType() == UserType.AMBASSADOR) {
                badgeService.applyBadgeForUser(user);
                userRepository.save(user);
                continue;
            }

            long attended = badgeService.countAttendedCompletedEvents(user.getId());

            if (attended >= 5) {
                if (user.getUserType() != UserType.AMBASSADOR) {
                    user.setUserType(UserType.AMBASSADOR);
                    emailService.sendAmbassadorPromotionEmail(user.getEmail(), user.getUserName());
                    log.info("User {} promoted to AMBASSADOR after event {} completion", user.getEmail(), eventId);
                }
            } else if (user.getUserType() == UserType.PARTICIPANT) {
                user.setUserType(UserType.CITIZEN);
            }

            badgeService.applyBadgeForUser(user);
            userRepository.save(user);
        }
    }
}
