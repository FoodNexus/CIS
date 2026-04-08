package com.civicplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * One row per (user, project). Composite PK is {@link ProjectVoteId} only — no
 * {@code @MapsId} associations, so Hibernate reliably emits INSERTs.
 */
@Entity
@Table(name = "project_vote")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ProjectVote {

    @EmbeddedId
    private ProjectVoteId id;

    @CreatedDate
    @Column(name = "voted_at", nullable = false, updatable = false)
    private LocalDateTime votedAt;
}
