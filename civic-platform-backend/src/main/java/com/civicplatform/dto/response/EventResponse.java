package com.civicplatform.dto.response;

import com.civicplatform.enums.EventStatus;
import com.civicplatform.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {
    
    private Long id;
    private String title;
    private LocalDateTime date;
    private EventType type;
    private Integer maxCapacity;
    private String description;
    private String location;
    private Integer currentParticipants;
    private Long organizerId;
    private String organizerName;
    private EventStatus status;
    private LocalDateTime createdAt;
    /** Set when this event is returned from the ML recommendation feed. */
    private Boolean isRecommended;
    /** Deterministic score used when feed is sorted by popularity. */
    private Double popularityScore;
    private List<EventParticipantResponse> participants;
    
    // Helper methods
    public boolean hasCapacity() {
        return currentParticipants < maxCapacity;
    }
    
    public Integer getAvailableSpots() {
        return maxCapacity - currentParticipants;
    }
    
    public double getCapacityPercentage() {
        if (maxCapacity == 0) return 0;
        return (double) currentParticipants / maxCapacity * 100;
    }
}
