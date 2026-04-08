package com.civicplatform.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BadgeProgressInfo {

    @JsonProperty("current_badge")
    private String currentBadge;

    @JsonProperty("events_attended")
    private long eventsAttended;

    @JsonProperty("next_badge")
    private String nextBadge;

    @JsonProperty("events_for_next")
    private Integer eventsForNext;

    @JsonProperty("events_remaining")
    private Integer eventsRemaining;
}
