package com.civicplatform.service.dto;

import com.civicplatform.enums.InvitationTier;
import com.civicplatform.enums.UserType;
import lombok.Builder;

/**
 * Lightweight immutable snapshot used for persisting matching run analytics/history.
 */
@Builder
public record InvitationCandidateSnapshot(
        Long citizenId,
        String citizenName,
        UserType citizenUserType,
        double compositeRate,
        double rawScore,
        InvitationTier invitationTier,
        boolean priorityFollowup,
        boolean directInvite
) {
}
