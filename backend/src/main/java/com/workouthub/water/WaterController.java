package com.workouthub.water;

import com.workouthub.common.security.AppUserPrincipal;
import com.workouthub.common.web.NotFoundException;
import com.workouthub.water.domain.WaterEntry;
import com.workouthub.water.domain.WaterEntryRepository;
import com.workouthub.water.dto.CreateWaterRequest;
import com.workouthub.water.dto.WaterDayDto;
import com.workouthub.water.dto.WaterEntryDto;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/water")
@Transactional
public class WaterController {

    private final WaterEntryRepository repo;

    public WaterController(WaterEntryRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public WaterDayDto get(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam(value = "date", required = false) String date) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate day = (date == null || date.isBlank())
                ? LocalDate.now(zone)
                : LocalDate.parse(date);
        Instant from = day.atStartOfDay(zone).toInstant();
        Instant to = day.plusDays(1).atStartOfDay(zone).toInstant();
        List<WaterEntry> rows = repo
                .findByUserIdAndConsumedAtBetweenOrderByConsumedAtDesc(
                        principal.userId(), from, to);
        int total = rows.stream().mapToInt(WaterEntry::getMl).sum();
        List<WaterEntryDto> dtos = rows.stream()
                .map(e -> new WaterEntryDto(e.getId(), e.getMl(), e.getConsumedAt()))
                .toList();
        return new WaterDayDto(day, total, dtos);
    }

    @PostMapping
    public ResponseEntity<WaterEntryDto> create(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CreateWaterRequest req) {
        WaterEntry e = new WaterEntry();
        e.setUserId(principal.userId());
        e.setMl(req.ml());
        e.setConsumedAt(req.consumedAt() == null ? Instant.now() : req.consumedAt());
        WaterEntry saved = repo.save(e);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new WaterEntryDto(saved.getId(), saved.getMl(), saved.getConsumedAt()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id) {
        WaterEntry e = repo.findByIdAndUserId(id, principal.userId())
                .orElseThrow(() -> new NotFoundException("Water entry not found: " + id));
        repo.delete(e);
        return ResponseEntity.noContent().build();
    }
}
