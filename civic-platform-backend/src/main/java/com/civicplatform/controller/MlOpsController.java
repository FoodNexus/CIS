package com.civicplatform.controller;

import com.civicplatform.dto.response.MlRetrainRunResponse;
import com.civicplatform.service.MlRetrainingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administrative operations for ML retraining.
 */
@RestController
@RequestMapping("/admin/ml")
@RequiredArgsConstructor
@Validated
@Tag(name = "ML Ops", description = "Administrative ML retraining endpoints")
@PreAuthorize("hasRole('ADMIN')")
public class MlOpsController {

    private final MlRetrainingService mlRetrainingService;

    @Operation(summary = "Trigger ML retrain job manually")
    @PostMapping("/retrain/trigger")
    public ResponseEntity<Void> triggerRetrain() {
        mlRetrainingService.triggerRetrain("manual");
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "List retrain job run history")
    @GetMapping("/retrain/runs")
    public ResponseEntity<Page<MlRetrainRunResponse>> listRuns(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
        return ResponseEntity.ok(mlRetrainingService.listRuns(PageRequest.of(page, size)));
    }
}
