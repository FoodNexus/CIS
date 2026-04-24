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
public class EventMatchingStatsResponse {

    private String runId;
    private Long eventId;
    private Integer candidateCount;
    private Integer directInviteCount;
    private Integer nurtureCount;
    private Double averageCompositeRate;
    private Double maxCompositeRate;
    private Long durationMs;
    private LocalDateTime createdAt;
}
