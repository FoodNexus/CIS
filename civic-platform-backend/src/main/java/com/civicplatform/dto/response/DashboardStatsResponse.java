package com.civicplatform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    
    private Map<String, Long> totalUsersByType;
    private Map<String, Long> totalCampaignsByStatus;
    private BigDecimal totalFundingAmount;
    private BigDecimal totalCo2Saved;
    private Integer totalMealsDistributed;
    private String mostActiveRegion;
    private Long totalProjects;
    private Long totalEvents;
    private Long activeVolunteers;
    private Long activeDonors;
    private Long activeAssociations;
    /** "online" or "offline" — Python ML recommendation service reachability. */
    private String mlServiceStatus;
}
