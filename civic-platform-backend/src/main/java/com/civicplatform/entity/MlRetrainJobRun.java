package com.civicplatform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Stores execution results of scheduled ML retraining jobs.
 */
@Entity
@Table(name = "ml_retrain_job_runs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MlRetrainJobRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at", nullable = false)
    private LocalDateTime finishedAt;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;
}
