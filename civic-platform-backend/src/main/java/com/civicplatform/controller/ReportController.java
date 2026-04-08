package com.civicplatform.controller;

import com.civicplatform.enums.UserType;
import com.civicplatform.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
@Tag(name = "Admin Reports", description = "Filtered user reports (PDF or CSV)")
@PreAuthorize("hasRole('ADMIN')")
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "Generate filtered user report as PDF or CSV")
    @GetMapping
    public ResponseEntity<byte[]> generateReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UserType type,
            @RequestParam String format) {

        byte[] report = reportService.generateReport(from, to, type, format);

        String ext = format != null ? format.trim().toLowerCase() : "pdf";
        String filename = "report-" + from + "-to-" + to
                + (type != null ? "-" + type : "")
                + "." + ext;

        MediaType mediaType = "pdf".equals(ext)
                ? MediaType.APPLICATION_PDF
                : MediaType.parseMediaType("text/csv");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .body(report);
    }
}
