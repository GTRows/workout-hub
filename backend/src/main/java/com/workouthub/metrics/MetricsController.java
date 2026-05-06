package com.workouthub.metrics;

import com.workouthub.common.security.AppUserPrincipal;
import com.workouthub.metrics.MetricsService.UpsertResult;
import com.workouthub.metrics.dto.BodyMetricDto;
import com.workouthub.metrics.dto.UpsertBodyMetricRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private final MetricsService service;

    public MetricsController(MetricsService service) {
        this.service = service;
    }

    @GetMapping
    public List<BodyMetricDto> list(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return service.list(principal.userId(), from, to);
    }

    @PostMapping
    public ResponseEntity<BodyMetricDto> upsert(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody UpsertBodyMetricRequest req) {
        UpsertResult result = service.upsert(principal.userId(), req);
        HttpStatus status = result.wasCreated() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.dto());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id) {
        service.delete(principal.userId(), id);
        return ResponseEntity.noContent().build();
    }
}
