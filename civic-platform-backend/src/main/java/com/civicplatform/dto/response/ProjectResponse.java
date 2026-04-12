package com.civicplatform.dto.response;

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
public class ProjectResponse {
    
    private Long id;
    private String title;
    private String description;
    private BigDecimal goalAmount;
    private BigDecimal currentAmount;
    private Integer voteCount;
    private String status;
    private LocalDate startDate;
    private LocalDate completionDate;
    private String finalReport;
    private String organizerType;
    private LocalDateTime createdAt;
    /** Creator user id — null for legacy rows before tracking was added. */
    private Long createdById;
    /** Set when this project is returned from the ML recommendation feed. */
    private Boolean isRecommended;
    private List<ProjectFundingResponse> fundings;
    
    // Helper methods
    public BigDecimal getFundingProgress() {
        if (goalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return currentAmount.divide(goalAmount, 2, BigDecimal.ROUND_HALF_UP);
    }
    
    public Integer getFundingPercentage() {
        if (goalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }
        return currentAmount.divide(goalAmount, 2, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100)).intValue();
    }
    
    public boolean isFullyFunded() {
        return currentAmount.compareTo(goalAmount) >= 0;
    }
}
