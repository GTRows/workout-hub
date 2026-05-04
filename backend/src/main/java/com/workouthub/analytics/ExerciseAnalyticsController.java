package com.workouthub.analytics;

import com.workouthub.analytics.dto.LastPerformanceDto;
import com.workouthub.analytics.dto.ProgressPointDto;
import com.workouthub.common.security.AppUserPrincipal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exercises/{exerciseId}")
@Validated
public class ExerciseAnalyticsController {

    private final ExerciseAnalyticsService service;

    public ExerciseAnalyticsController(ExerciseAnalyticsService service) {
        this.service = service;
    }

    @GetMapping("/last-performance")
    public ResponseEntity<LastPerformanceDto> lastPerformance(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID exerciseId) {
        return service.lastPerformance(principal.userId(), exerciseId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/progress")
    public List<ProgressPointDto> progress(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID exerciseId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
        return service.progress(principal.userId(), exerciseId, limit);
    }
}
