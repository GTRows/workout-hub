package com.workouthub.exports;

import com.workouthub.common.security.AppUserPrincipal;
import com.workouthub.exports.dto.ClaudeSummaryDto;
import com.workouthub.exports.dto.FullExportDto;
import com.workouthub.exports.dto.FullExportDto.MetricRow;
import com.workouthub.exports.dto.FullExportDto.PlanSection;
import com.workouthub.exports.dto.FullExportDto.SessionSection;
import com.workouthub.exports.dto.FullExportDto.SupplementRow;
import com.workouthub.exports.dto.FullExportDto.UserSection;
import com.workouthub.exports.dto.ImportResultDto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/export")
@Validated
public class ExportController {

    private final ExportService service;
    private final FullExportService fullExport;
    private final FullImportService fullImport;

    public ExportController(
            ExportService service,
            FullExportService fullExport,
            FullImportService fullImport) {
        this.service = service;
        this.fullExport = fullExport;
        this.fullImport = fullImport;
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

    @GetMapping("/full")
    public ResponseEntity<FullExportDto> fullDump(
            @AuthenticationPrincipal AppUserPrincipal principal) {
        FullExportDto body = fullExport.build(principal.userId());
        String filename = "workouthub-full-" + LocalDate.now() + ".json";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(body);
    }

    @PostMapping("/import")
    public ImportResultDto importDump(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestBody FullExportDto dump) {
        return fullImport.importDump(principal.userId(), dump);
    }

    @GetMapping("/profile")
    public UserSection profileSlice(@AuthenticationPrincipal AppUserPrincipal principal) {
        return fullExport.buildUserSection(principal.userId());
    }

    @GetMapping("/plans")
    public List<PlanSection> plansSlice(@AuthenticationPrincipal AppUserPrincipal principal) {
        return fullExport.buildPlans(principal.userId());
    }

    @GetMapping("/sessions")
    public List<SessionSection> sessionsSlice(
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return fullExport.buildSessions(principal.userId());
    }

    @GetMapping("/metrics")
    public List<MetricRow> metricsSlice(@AuthenticationPrincipal AppUserPrincipal principal) {
        return fullExport.buildMetrics(principal.userId());
    }

    @GetMapping("/supplements")
    public List<SupplementRow> supplementsSlice(
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return fullExport.buildSupplements(principal.userId());
    }
}
