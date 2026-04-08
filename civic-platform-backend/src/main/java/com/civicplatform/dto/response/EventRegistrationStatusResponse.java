package com.civicplatform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRegistrationStatusResponse {

    private boolean registered;
    /** Participant status when registered (e.g. REGISTERED, COMPLETED); null if not registered */
    private String status;
}
