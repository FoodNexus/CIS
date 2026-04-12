package com.civicplatform.dto.response;

import com.civicplatform.enums.CampaignStatus;
import com.civicplatform.enums.CampaignType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignResponse {
    
    private Long id;
    private String name;
    private BigDecimal neededAmount;
    private CampaignType type;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer goalKg;
    private Integer goalMeals;
    private BigDecimal goalAmount;
    private Integer currentKg;
    private Integer currentMeals;
    private CampaignStatus status;
    private String hashtag;
    private LocalDateTime createdAt;
    private Long createdById;
    private String createdByName;
    private Integer voteCount;
    /** Set when this campaign is returned from the ML recommendation feed. */
    private Boolean isRecommended;
    private List<PostResponse> posts;
    
    // Helper methods
    public BigDecimal getProgressPercentage() {
        if (goalKg == null || goalKg == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf((double) currentKg / goalKg * 100);
    }
}
