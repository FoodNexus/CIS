package com.civicplatform.controller;

import com.civicplatform.dto.response.AmbassadorInfluenceResponse;
import com.civicplatform.service.AmbassadorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/ambassadors")
@RequiredArgsConstructor
@Tag(name = "Ambassador Ranking", description = "Ambassador ranking APIs")
public class AmbassadorController {

    private final AmbassadorService ambassadorService;

    @Operation(summary = "Get ambassadors ranking")
    @GetMapping("/ranking")
    public ResponseEntity<List<AmbassadorInfluenceResponse>> getRanking(
            @RequestParam(name = "sort", defaultValue = "influence") String sort) {
        if ("influence".equalsIgnoreCase(sort)) {
            return ResponseEntity.ok(ambassadorService.getRankingByInfluence());
        }
        return ResponseEntity.ok(ambassadorService.getRankingByInfluence());
    }
}
