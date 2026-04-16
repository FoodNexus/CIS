package com.civicplatform.entity;

import com.civicplatform.enums.InvitationTier;
import com.civicplatform.enums.MatchStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "event_citizen_invitations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_eci_event_citizen", columnNames = {"event_id", "citizen_id"}),
                @UniqueConstraint(name = "uk_eci_token", columnNames = "invitation_token")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventCitizenInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "citizen_id", nullable = false)
    private User citizen;

    @Column(name = "match_score", nullable = false)
    private Double matchScore;

    /** Normalized 0–100 rate used for thresholds and UI. */
    @Column(name = "composite_rate")
    private Double compositeRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "invitation_tier", length = 40)
    private InvitationTier invitationTier;

    @Column(name = "priority_followup", nullable = false)
    @Builder.Default
    private boolean priorityFollowup = false;

    @Column(name = "feature_breakdown_json", columnDefinition = "TEXT")
    private String featureBreakdownJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(32)")
    @Builder.Default
    private MatchStatus status = MatchStatus.INVITED;

    @Column(name = "invited_at")
    private LocalDateTime invitedAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "invitation_token", nullable = false, length = 255)
    private String invitationToken;
}
