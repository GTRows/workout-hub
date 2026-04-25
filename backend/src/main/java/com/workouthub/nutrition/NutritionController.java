package com.workouthub.nutrition;

import com.workouthub.common.security.AppUserPrincipal;
import com.workouthub.nutrition.dto.CreateNutritionEntryRequest;
import com.workouthub.nutrition.dto.FoodItemDto;
import com.workouthub.nutrition.dto.NutritionEntryDto;
import com.workouthub.nutrition.dto.UpdateNutritionEntryRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Validated
public class NutritionController {

    private final NutritionService service;

    public NutritionController(NutritionService service) {
        this.service = service;
    }

    @GetMapping("/foods")
    public List<FoodItemDto> searchFoods(
            @RequestParam(value = "q", defaultValue = "") String q,
            @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.searchFoods(q, size);
    }

    @GetMapping("/nutrition")
    public List<NutritionEntryDto> list(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam(value = "date", required = false) String date) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate day = (date == null || date.isBlank())
                ? LocalDate.now(zone)
                : LocalDate.parse(date);
        Instant from = day.atStartOfDay(zone).toInstant();
        Instant to = day.plusDays(1).atStartOfDay(zone).toInstant();
        return service.listForUserBetween(principal.userId(), from, to);
    }

    @PostMapping("/nutrition")
    public ResponseEntity<NutritionEntryDto> create(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CreateNutritionEntryRequest req) {
        NutritionEntryDto body = service.create(principal.userId(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PutMapping("/nutrition/{id}")
    public NutritionEntryDto update(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateNutritionEntryRequest req) {
        return service.update(principal.userId(), id, req);
    }

    @DeleteMapping("/nutrition/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id) {
        service.delete(principal.userId(), id);
        return ResponseEntity.noContent().build();
    }
}
