package com.civicplatform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Deterministic project-level workflow insight built from existing CIS scores.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectInsightResponse {
    private Long projectId;
    private String projectTitle;

    private Double workflowScore;
    private String workflowStage;
    private String recommendedAction;

    private Double fundingComponent;
    private Double voteComponent;
    private Double postPopularityComponent;
    private Double eventPopularityComponent;
    private Double ambassadorInfluenceComponent;

    private Long topPostId;
    private Long topEventId;
    private Long topAmbassadorUserId;
}
