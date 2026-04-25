package com.civicplatform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response item for ambassador influence ranking.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AmbassadorInfluenceResponse {
    private Long userId;
    private String userName;
    private Double influenceScore;
    private Long actionsCount;
    private Long eventsCount;
    private Long postsCount;
    private Long totalEngagement;
    private Long referralsCount;
}
