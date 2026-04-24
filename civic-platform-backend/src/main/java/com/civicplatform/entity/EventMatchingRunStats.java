package com.civicplatform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Stores one immutable summary row per invitation matching run.
 */
@Entity
@Table(name = "event_matching_run_stats")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventMatchingRunStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "run_id", nullable = false, length = 64, unique = true)
    private String runId;

    @Column(name = "candidate_count", nullable = false)
    private Integer candidateCount;

    @Column(name = "direct_invite_count", nullable = false)
    private Integer directInviteCount;

    @Column(name = "nurture_count", nullable = false)
    private Integer nurtureCount;

    @Column(name = "average_composite_rate", nullable = false)
    private Double averageCompositeRate;

    @Column(name = "max_composite_rate", nullable = false)
    private Double maxCompositeRate;

    @Column(name = "duration_ms", nullable = false)
    private Long durationMs;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
