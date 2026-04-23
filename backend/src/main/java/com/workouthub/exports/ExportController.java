package com.workouthub.exports;

import com.workouthub.common.security.AppUserPrincipal;
import com.workouthub.exports.dto.ClaudeSummaryDto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/export")
@Validated
public class ExportController {

    private final ExportService service;

    public ExportController(ExportService service) {
        this.service = service;
    }

    @GetMapping("/claude-summary")
    public ResponseEntity<ClaudeSummaryDto> claudeSummary(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam(defaultValue = "30") @Min(1) @Max(365) int days) {
        ClaudeSummaryDto body = service.buildSummary(principal.userId(), days);
        String filename = "workouthub-claude-" + body.period().to() + ".json";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(body);
    }
}
