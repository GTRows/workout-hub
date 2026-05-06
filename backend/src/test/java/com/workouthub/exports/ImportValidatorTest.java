package com.workouthub.exports;

import static org.assertj.core.api.Assertions.assertThat;

import com.workouthub.exports.ImportValidator.ValidationReport;
import com.workouthub.exports.dto.FullExportDto;
import com.workouthub.exports.dto.FullExportDto.DayExerciseRow;
import com.workouthub.exports.dto.FullExportDto.DayRow;
import com.workouthub.exports.dto.FullExportDto.PlanSection;
import com.workouthub.exports.dto.FullExportDto.SessionSection;
import com.workouthub.exports.dto.FullExportDto.SetRow;
import com.workouthub.exports.dto.FullExportDto.SupplementRow;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ImportValidatorTest {

    @Test
    void nullPayloadIsAnError() {
        ValidationReport r = ImportValidator.validate(null);
        assertThat(r.hasErrors()).isTrue();
        assertThat(r.errors()).contains("payload is null");
    }

    @Test
    void unsupportedSchemaVersionIsAnError() {
        FullExportDto dump = emptyDump(99);
        ValidationReport r = ImportValidator.validate(dump);
        assertThat(r.hasErrors()).isTrue();
        assertThat(r.errors().get(0)).contains("unsupported schemaVersion 99");
    }

    @Test
    void multipleActivePlansIsAnError() {
        PlanSection a = new PlanSection(UUID.randomUUID(), "A", true, List.of());
        PlanSection b = new PlanSection(UUID.randomUUID(), "B", true, List.of());
        FullExportDto dump = withPlans(List.of(a, b));
        ValidationReport r = ImportValidator.validate(dump);
        assertThat(r.hasErrors()).isTrue();
        assertThat(r.errors().get(0)).contains("at most one plan may be active");
    }

    @Test
    void unknownSupplementTimingIsAWarningNotError() {
        SupplementRow s = new SupplementRow(
                UUID.randomUUID(), "Odd", "5g", "midnight_snack", true);
        FullExportDto dump = withSupplements(List.of(s));
        ValidationReport r = ImportValidator.validate(dump);
        assertThat(r.hasErrors()).isFalse();
        assertThat(r.warnings()).anyMatch(w -> w.contains("unknown timing 'midnight_snack'"));
    }

    @Test
    void unknownFocusIsAWarning() {
        DayRow day = new DayRow(
                UUID.randomUUID(), (short) 1, "Day 1", "pilates", 60, List.of());
        PlanSection plan = new PlanSection(UUID.randomUUID(), "P", false, List.of(day));
        FullExportDto dump = withPlans(List.of(plan));
        ValidationReport r = ImportValidator.validate(dump);
        assertThat(r.hasErrors()).isFalse();
        assertThat(r.warnings()).anyMatch(w -> w.contains("unknown focus 'pilates'"));
    }

    @Test
    void dayExerciseMissingExerciseIdIsAWarning() {
        DayExerciseRow row = new DayExerciseRow(
                UUID.randomUUID(), null, 0, 3, 8, 10, null, 120, null);
        DayRow day = new DayRow(
                UUID.randomUUID(), (short) 1, "D", "push", 60, List.of(row));
        PlanSection plan = new PlanSection(UUID.randomUUID(), "P", false, List.of(day));
        FullExportDto dump = withPlans(List.of(plan));
        ValidationReport r = ImportValidator.validate(dump);
        assertThat(r.warnings()).anyMatch(w -> w.contains("no exerciseId"));
    }

    @Test
    void sessionWithNoSetsIsAWarning() {
        SessionSection s = new SessionSection(
                UUID.randomUUID(), null,
                Instant.parse("2026-04-20T10:00:00Z"),
                Instant.parse("2026-04-20T11:00:00Z"),
                null, null, null, List.of());
        FullExportDto dump = withSessions(List.of(s));
        ValidationReport r = ImportValidator.validate(dump);
        assertThat(r.warnings()).anyMatch(w -> w.contains("no sets"));
    }

    @Test
    void setWithMissingExerciseIdIsAWarning() {
        SetRow set = new SetRow(
                UUID.randomUUID(), null, (short) 1, (short) 10,
                new BigDecimal("60"), null, true, null, null);
        SessionSection session = new SessionSection(
                UUID.randomUUID(), null,
                Instant.parse("2026-04-20T10:00:00Z"),
                Instant.parse("2026-04-20T11:00:00Z"),
                null, null, null, List.of(set));
        FullExportDto dump = withSessions(List.of(session));
        ValidationReport r = ImportValidator.validate(dump);
        assertThat(r.warnings()).anyMatch(w -> w.contains("has no exerciseId"));
    }

    @Test
    void largeSessionCountEmitsASuggestion() {
        List<SessionSection> many = new ArrayList<>();
        IntStream.range(0, 1001).forEach(i -> many.add(new SessionSection(
                UUID.randomUUID(), null,
                Instant.parse("2026-04-20T10:00:00Z"),
                Instant.parse("2026-04-20T11:00:00Z"),
                null, null, null,
                List.of(new SetRow(
                        UUID.randomUUID(), UUID.randomUUID(),
                        (short) 1, (short) 10, new BigDecimal("60"),
                        null, true, null, null)))));
        FullExportDto dump = withSessions(many);
        ValidationReport r = ImportValidator.validate(dump);
        assertThat(r.suggestions()).anyMatch(s -> s.contains("1001 sessions"));
    }

    @Test
    void minimalEmptyPayloadHasNoErrorsOrWarnings() {
        ValidationReport r = ImportValidator.validate(emptyDump(1));
        assertThat(r.hasErrors()).isFalse();
        assertThat(r.warnings()).isEmpty();
    }

    private static FullExportDto emptyDump(int schemaVersion) {
        return new FullExportDto(schemaVersion, Instant.EPOCH, null,
                List.of(), List.of(), List.of(), List.of());
    }

    private static FullExportDto withPlans(List<PlanSection> plans) {
        return new FullExportDto(1, Instant.EPOCH, null,
                plans, List.of(), List.of(), List.of());
    }

    private static FullExportDto withSessions(List<SessionSection> sessions) {
        return new FullExportDto(1, Instant.EPOCH, null,
                List.of(), sessions, List.of(), List.of());
    }

    private static FullExportDto withSupplements(List<SupplementRow> supplements) {
        return new FullExportDto(1, Instant.EPOCH, null,
                List.of(), List.of(), List.of(), supplements);
    }
}
