package com.workouthub.analytics;

import com.workouthub.analytics.dto.HeatmapDayDto;
import com.workouthub.analytics.dto.OneRmPointDto;
import com.workouthub.analytics.dto.PrDto;
import com.workouthub.analytics.dto.StreakDto;
import com.workouthub.analytics.dto.WeeklyVolumeDto;
import com.workouthub.common.security.AppUserPrincipal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@Validated
public class AnalyticsController {

    private final AnalyticsService service;

    public AnalyticsController(AnalyticsService service) {
        this.service = service;
    }

    @GetMapping("/volume")
    public List<WeeklyVolumeDto> volume(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam(defaultValue = "12") @Min(1) @Max(52) int weeks) {
        return service.weeklyVolume(principal.userId(), weeks);
    }

    @GetMapping("/one-rm/{exerciseId}")
    public List<OneRmPointDto> oneRepMax(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID exerciseId) {
        return service.oneRepMax(principal.userId(), exerciseId);
    }

    @GetMapping("/streak")
    public StreakDto streak(@AuthenticationPrincipal AppUserPrincipal principal) {
        return service.streak(principal.userId());
    }

    @GetMapping("/prs")
    public List<PrDto> personalRecords(@AuthenticationPrincipal AppUserPrincipal principal) {
        return service.personalRecords(principal.userId());
    }

    @GetMapping("/heatmap")
    public List<HeatmapDayDto> heatmap(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam(defaultValue = "12") @Min(1) @Max(52) int weeks) {
        return service.heatmap(principal.userId(), weeks);
    }
}
