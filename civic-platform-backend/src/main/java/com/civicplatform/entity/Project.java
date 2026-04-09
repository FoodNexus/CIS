package com.civicplatform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "project")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Project {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "goal_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal goalAmount;
    
    @Column(name = "current_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal currentAmount = BigDecimal.ZERO;
    
    @Column(name = "vote_count", nullable = false)
    @Builder.Default
    private Integer voteCount = 0;
    
    @Column(nullable = false)
    @Builder.Default
    private String status = "SUBMITTED";
    
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Column(name = "completion_date")
    private LocalDate completionDate;
    
    @Column(name = "final_report", columnDefinition = "TEXT")
    private String finalReport;
    
    @Column(name = "organizer_type")
    private String organizerType;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;
    
    // Relationships
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProjectFunding> fundings;
    
    // Helper methods
    public void submit() {
        this.status = "SUBMITTED";
        this.startDate = LocalDate.now();
    }
    
    public void vote() {
        this.voteCount++;
    }
    
    public void fund(BigDecimal amount) {
        this.currentAmount = this.currentAmount.add(amount);
    }
    
    public void complete(String report) {
        this.status = "COMPLETED";
        this.completionDate = LocalDate.now();
        this.finalReport = report;
    }
    
    public boolean isFullyFunded() {
        return currentAmount.compareTo(goalAmount) >= 0;
    }
    
    public BigDecimal getFundingProgress() {
        if (goalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return currentAmount.divide(goalAmount, 2, java.math.RoundingMode.HALF_UP);
    }
    
    public int getFundingPercentage() {
        if (goalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }
        return currentAmount.multiply(BigDecimal.valueOf(100))
                .divide(goalAmount, 0, java.math.RoundingMode.HALF_UP)
                .intValue();
    }
}
