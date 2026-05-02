package com.workouthub.exports;

import com.workouthub.common.security.AppUserPrincipal;
import com.workouthub.exports.dto.ClaudeSummaryDto;
import com.workouthub.exports.dto.CsvImportResultDto;
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
    private final CsvExportService csvExport;
    private final CsvImportService csvImport;
    private final IcsExportService icsExport;

    public ExportController(
            ExportService service,
            FullExportService fullExport,
            FullImportService fullImport,
            CsvExportService csvExport,
            CsvImportService csvImport,
            IcsExportService icsExport) {
        this.service = service;
        this.fullExport = fullExport;
        this.fullImport = fullImport;
        this.csvExport = csvExport;
        this.csvImport = csvImport;
        this.icsExport = icsExport;
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

    @PostMapping("/import/profile")
    public ImportResultDto importProfileSlice(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestBody UserSection section) {
        int updated = fullImport.importProfile(principal.userId(), section);
        return new ImportResultDto(updated, 0, 0, 0, 0, "");
    }

    @PostMapping("/import/metrics")
    public ImportResultDto importMetricsSlice(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestBody List<MetricRow> rows) {
        int inserted = fullImport.replaceMetricsSection(principal.userId(), rows);
        return new ImportResultDto(0, inserted, 0, 0, 0, "");
    }

    @PostMapping("/import/supplements")
    public ImportResultDto importSupplementsSlice(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestBody List<SupplementRow> rows) {
        int inserted = fullImport.replaceSupplementsSection(principal.userId(), rows);
        return new ImportResultDto(0, 0, inserted, 0, 0, "");
    }

    @PostMapping("/import/plans")
    public ImportResultDto importPlansSlice(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestBody List<PlanSection> rows) {
        int inserted = fullImport.replacePlansSection(principal.userId(), rows);
        return new ImportResultDto(0, 0, 0, inserted, 0, "");
    }

    @GetMapping(value = "/plan.ics", produces = "text/calendar; charset=utf-8")
    public ResponseEntity<String> planIcs(
            @AuthenticationPrincipal AppUserPrincipal principal) {
        String ics = icsExport.buildActivePlanIcs(principal.userId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/calendar; charset=utf-8")
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"workouthub-plan.ics\"")
                .body(ics);
    }

    @PostMapping(value = "/import/csv", consumes = "text/csv")
    public CsvImportResultDto importSessionsCsv(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestBody(required = false) String csv) {
        return csvImport.importSessionsCsv(principal.userId(), csv == null ? "" : csv);
    }

    @GetMapping(value = "/csv/sessions", produces = "text/csv; charset=utf-8")
    public ResponseEntity<String> sessionsCsv(
            @AuthenticationPrincipal AppUserPrincipal principal) {
        String csv = csvExport.buildSessionsCsv(principal.userId());
        String filename = "workouthub-sessions-" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=utf-8")
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(csv);
    }

    @PostMapping("/import/sessions")
    public ImportResultDto importSessionsSlice(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestBody List<SessionSection> rows) {
        int inserted = fullImport.replaceSessionsSection(principal.userId(), rows);
        return new ImportResultDto(0, 0, 0, 0, inserted, "");
    }
}
