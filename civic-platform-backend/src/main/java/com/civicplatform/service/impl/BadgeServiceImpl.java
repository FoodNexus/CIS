package com.civicplatform.service.impl;

import com.civicplatform.dto.response.BadgeProgressInfo;
import com.civicplatform.entity.User;
import com.civicplatform.enums.Badge;
import com.civicplatform.repository.EventParticipantRepository;
import com.civicplatform.service.BadgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class BadgeServiceImpl implements BadgeService {

    private final EventParticipantRepository eventParticipantRepository;

    @Override
    public long countAttendedCompletedEvents(Long userId) {
        return eventParticipantRepository.countAttendedCompletedEventsByUser(userId);
    }

    @Override
    public Badge badgeForAttendedCount(long attendedCount) {
        if (attendedCount >= 8) {
            return Badge.PLATINUM;
        }
        if (attendedCount >= 5) {
            return Badge.GOLD;
        }
        if (attendedCount >= 3) {
            return Badge.SILVER;
        }
        if (attendedCount >= 1) {
            return Badge.BRONZE;
        }
        return Badge.NONE;
    }

    @Override
    public void applyBadgeForUser(User user) {
        long attended = countAttendedCompletedEvents(user.getId());
        user.setPoints((int) Math.min(attended, Integer.MAX_VALUE));

        Badge computed = badgeForAttendedCount(attended);
        Badge current = user.getBadge() != null ? user.getBadge() : Badge.NONE;

        if (computed.ordinal() > current.ordinal()) {
            user.setBadge(computed);
            user.setAwardedDate(LocalDate.now());
        }
    }

    @Override
    public BadgeProgressInfo computeBadgeProgress(Badge storedBadge, long attendedCount) {
        Badge current = storedBadge != null ? storedBadge : Badge.NONE;
        String currentName = current.name();

        if (current == Badge.PLATINUM) {
            return BadgeProgressInfo.builder()
                    .currentBadge(currentName)
                    .eventsAttended(attendedCount)
                    .nextBadge(null)
                    .eventsForNext(8)
                    .eventsRemaining(0)
                    .build();
        }

        Badge next = switch (current) {
            case NONE -> Badge.BRONZE;
            case BRONZE -> Badge.SILVER;
            case SILVER -> Badge.GOLD;
            case GOLD -> Badge.PLATINUM;
            default -> Badge.PLATINUM;
        };

        int threshold = thresholdForBadge(next);
        int remaining = (int) Math.max(0, threshold - attendedCount);

        return BadgeProgressInfo.builder()
                .currentBadge(currentName)
                .eventsAttended(attendedCount)
                .nextBadge(next.name())
                .eventsForNext(threshold)
                .eventsRemaining(remaining)
                .build();
    }

    private static int thresholdForBadge(Badge badge) {
        return switch (badge) {
            case BRONZE -> 1;
            case SILVER -> 3;
            case GOLD -> 5;
            case PLATINUM -> 8;
            default -> 1;
        };
    }
}
