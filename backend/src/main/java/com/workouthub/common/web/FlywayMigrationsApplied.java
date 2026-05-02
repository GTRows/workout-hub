package com.workouthub.common.web;

import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Component;

@Component
class FlywayMigrationsApplied {

    private final Flyway flyway;

    FlywayMigrationsApplied(Flyway flyway) {
        this.flyway = flyway;
    }

    boolean allApplied() {
        return flyway.info().pending().length == 0;
    }
}
