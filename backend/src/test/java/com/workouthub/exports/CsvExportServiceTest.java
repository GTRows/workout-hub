package com.workouthub.exports;

import static org.assertj.core.api.Assertions.assertThat;

import com.workouthub.exercises.domain.Exercise;
import com.workouthub.sessions.domain.SessionSet;
import com.workouthub.sessions.domain.WorkoutSession;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CsvExportServiceTest {

    @Test
    void emptyHistoryEmitsJustTheHeader() {
        String csv = CsvExportService.buildCsv(List.of());
        assertThat(csv.trim()).isEqualTo(CsvExportService.HEADER);
    }

    @Test
    void singleSessionWithTwoSetsEmitsTwoRowsInStrongColumnOrder() throws Exception {
        WorkoutSession s = session(Instant.parse("2026-04-20T09:00:00Z"), "felt strong");
        s.getSets().add(set(s, "Bench Press", (short) 1, (short) 8, new BigDecimal("80.00"), null));
        s.getSets().add(set(s, "Bench Press", (short) 2, (short) 8, new BigDecimal("80.00"), "tough"));

        String csv = CsvExportService.buildCsv(List.of(s));
        String[] lines = csv.split("\n");

        assertThat(lines[0]).isEqualTo(CsvExportService.HEADER);
        assertThat(lines[1]).startsWith("2026-04-20 ");
        assertThat(lines[1]).contains(",Bench Press,1,80,8,,felt strong");
        assertThat(lines[2]).contains(",Bench Press,2,80,8,tough,felt strong");
    }

    @Test
    void quotesFieldsContainingCommas() throws Exception {
        WorkoutSession s = session(Instant.parse("2026-04-20T09:00:00Z"), "notes, with comma");
        s.getSets().add(set(s, "Bench Press", (short) 1, (short) 8, new BigDecimal("80"), null));

        String csv = CsvExportService.buildCsv(List.of(s));

        assertThat(csv).contains("\"notes, with comma\"");
    }

    private static WorkoutSession session(Instant startedAt, String notes) throws Exception {
        WorkoutSession s = new WorkoutSession();
        setField(s, "id", UUID.randomUUID());
        s.setStartedAt(startedAt);
        s.setNotes(notes);
        return s;
    }

    private static SessionSet set(
            WorkoutSession session, String exerciseName, short setNumber,
            short reps, BigDecimal weight, String notes) throws Exception {
        Exercise ex = new Exercise();
        setField(ex, "id", UUID.randomUUID());
        setField(ex, "nameEn", exerciseName);

        SessionSet set = new SessionSet();
        setField(set, "id", UUID.randomUUID());
        set.setSession(session);
        set.setExercise(ex);
        set.setSetNumber(setNumber);
        set.setRepsDone(reps);
        set.setWeightKg(weight);
        set.setCompleted(true);
        set.setNotes(notes);
        return set;
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = findField(target.getClass(), fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> c = type;
        while (c != null) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
