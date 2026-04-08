package com.civicplatform.service;

import com.civicplatform.dto.response.BadgeProgressInfo;
import com.civicplatform.entity.User;
import com.civicplatform.enums.Badge;

public interface BadgeService {

    long countAttendedCompletedEvents(Long userId);

    /**
     * Badge from total attended count (thresholds: 8/5/3/1/0).
     */
    Badge badgeForAttendedCount(long attendedCount);

    /**
     * Updates user's badge only if the new level is higher; sets awardedDate on increase.
     * Syncs points to attended count.
     */
    void applyBadgeForUser(User user);

    BadgeProgressInfo computeBadgeProgress(Badge storedBadge, long attendedCount);
}
