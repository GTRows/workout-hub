package com.workouthub.health;

import com.workouthub.common.security.AppUserPrincipal;
import com.workouthub.health.dto.HealthImportResultDto;
import java.io.IOException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/health/import")
public class HealthImportController {

    private final HealthImportService service;

    public HealthImportController(HealthImportService service) {
        this.service = service;
    }

    @PostMapping("/apple")
    public ResponseEntity<HealthImportResultDto> importApple(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "bodyMass", defaultValue = "true") boolean bodyMass,
            @RequestParam(value = "workouts", defaultValue = "true") boolean workouts) throws Exception {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        try (var stream = file.getInputStream()) {
            HealthImportResultDto result = service.importApple(
                    principal.userId(), stream, bodyMass, workouts);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/google-fit")
    public ResponseEntity<HealthImportResultDto> importGoogleFit(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "bodyMass", defaultValue = "true") boolean bodyMass,
            @RequestParam(value = "workouts", defaultValue = "true") boolean workouts) throws Exception {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        try (var stream = file.getInputStream()) {
            HealthImportResultDto result = service.importGoogleFit(
                    principal.userId(), stream, bodyMass, workouts);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/fit")
    public ResponseEntity<HealthImportResultDto> importFit(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        try (var stream = file.getInputStream()) {
            HealthImportResultDto result = service.importGarminFit(principal.userId(), stream);
            return ResponseEntity.ok(result);
        } catch (IOException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
