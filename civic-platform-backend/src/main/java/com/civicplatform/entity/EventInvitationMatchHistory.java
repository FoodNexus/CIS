package com.civicplatform.entity;

import com.civicplatform.enums.InvitationTier;
import com.civicplatform.enums.UserType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Stores candidate-level score snapshots for each matching run.
 */
@Entity
@Table(name = "event_invitation_match_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventInvitationMatchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "run_id", nullable = false, length = 64)
    private String runId;

    @Column(name = "citizen_id", nullable = false)
    private Long citizenId;

    @Column(name = "citizen_name", nullable = false, length = 255)
    private String citizenName;

    @Enumerated(EnumType.STRING)
    @Column(name = "citizen_user_type", length = 32)
    private UserType citizenUserType;

    @Column(name = "composite_rate", nullable = false)
    private Double compositeRate;

    @Column(name = "raw_score", nullable = false)
    private Double rawScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "invitation_tier", length = 40)
    private InvitationTier invitationTier;

    @Column(name = "priority_followup", nullable = false)
    private boolean priorityFollowup;

    @Column(name = "selected_for_direct_invite", nullable = false)
    private boolean selectedForDirectInvite;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
