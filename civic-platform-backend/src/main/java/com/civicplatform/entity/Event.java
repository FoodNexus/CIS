package com.civicplatform.entity;

import com.civicplatform.enums.EventStatus;
import com.civicplatform.enums.EventType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "event")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Event {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false)
    private LocalDateTime date;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType type;
    
    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    private String location;
    
    @Column(name = "current_participants", nullable = false)
    @Builder.Default
    private Integer currentParticipants = 0;
    
    @Column(name = "organizer_id", nullable = false)
    private Long organizerId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EventStatus status = EventStatus.UPCOMING;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    // Relationships
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<EventParticipant> participants;
    
    // Helper methods
    public boolean hasCapacity() {
        int cur = currentParticipants == null ? 0 : currentParticipants;
        int max = maxCapacity == null ? Integer.MAX_VALUE : maxCapacity;
        return cur < max;
    }

    public void incrementParticipants() {
        if (!hasCapacity()) {
            return;
        }
        int cur = currentParticipants == null ? 0 : currentParticipants;
        currentParticipants = cur + 1;
    }

    public void decrementParticipants() {
        int cur = currentParticipants == null ? 0 : currentParticipants;
        if (cur > 0) {
            currentParticipants = cur - 1;
        }
    }
    
    public void cancel() {
        this.status = EventStatus.CANCELLED;
    }
    
    public void start() {
        this.status = EventStatus.ONGOING;
    }
    
    public void complete() {
        this.status = EventStatus.COMPLETED;
    }
}
