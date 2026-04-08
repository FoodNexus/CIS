package com.civicplatform.dto.request;

import com.civicplatform.enums.EventStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventStatusUpdateRequest {

    @NotNull
    private EventStatus status;
}
