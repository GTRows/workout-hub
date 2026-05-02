package com.workouthub.common.web;

import com.workouthub.common.web.dto.HealthStatusDto;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final DataSource dataSource;
    private final FlywayMigrationsApplied flywayCheck;

    public HealthController(DataSource dataSource, FlywayMigrationsApplied flywayCheck) {
        this.dataSource = dataSource;
        this.flywayCheck = flywayCheck;
    }

    @GetMapping("/livez")
    public ResponseEntity<HealthStatusDto> livez() {
        return ResponseEntity.ok(new HealthStatusDto("alive", null));
    }

    @GetMapping("/healthz")
    public ResponseEntity<HealthStatusDto> healthz() {
        try (Connection c = dataSource.getConnection()) {
            if (!c.isValid(1)) {
                log.warn("Readiness probe failed: db connection invalid");
                return ResponseEntity.status(503)
                        .body(new HealthStatusDto("unready", "db_unreachable"));
            }
        } catch (SQLException ex) {
            log.warn("Readiness probe failed: db error {}", ex.getClass().getSimpleName());
            return ResponseEntity.status(503)
                    .body(new HealthStatusDto("unready", "db_error: " + ex.getClass().getSimpleName()));
        }
        if (!flywayCheck.allApplied()) {
            log.warn("Readiness probe failed: pending migrations");
            return ResponseEntity.status(503)
                    .body(new HealthStatusDto("unready", "migrations_pending"));
        }
        return ResponseEntity.ok(new HealthStatusDto("ready", null));
    }
}
