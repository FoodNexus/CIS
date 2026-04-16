package com.civicplatform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventInvitationResponse {

    private Long invitationId;
    private Long eventId;
    private String eventTitle;
    private Long citizenId;
    private String citizenFullName;
    private String citizenUserType;
    private String citizenBadge;
    private int citizenEventsAttended;
    private double matchScore;
    /** Normalized 0–100 composite rate (multi-feature). */
    private Double compositeRate;
    private double matchScorePercent;
    /** PRIORITY_IMMEDIATE, STANDARD_INVITE (persisted); NURTURE users do not receive an invitation row. */
    private String invitationTier;
    private boolean priorityFollowup;
    /** JSON breakdown of feature contributions for transparency. */
    private String featureBreakdownJson;
    private String status;
    private LocalDateTime invitedAt;
    private LocalDateTime respondedAt;
    /** Present for the invitee's own list; omitted for organizer view. */
    private String invitationToken;
    /** Donor / association display name for invitation cards. */
    private String donorAssociationName;
}
