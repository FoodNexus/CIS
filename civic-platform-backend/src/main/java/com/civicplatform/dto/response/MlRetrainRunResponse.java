package com.civicplatform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MlRetrainRunResponse {
    private Long id;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String status;
    private String message;
}
