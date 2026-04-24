package com.workouthub.exports;

import static org.assertj.core.api.Assertions.assertThat;

import com.workouthub.workouts.domain.WorkoutDay;
import com.workouthub.workouts.domain.WorkoutFocus;
import com.workouthub.workouts.domain.WorkoutPlan;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IcsExportServiceTest {

    @Test
    void sevenDayPlanEmitsSevenVeventsWrappedInVcalendar() throws Exception {
        WorkoutPlan plan = plan("Weekly 7");
        for (int dow = 1; dow <= 7; dow++) {
            plan.getDays().add(day(plan, (short) dow, "Day " + dow, WorkoutFocus.PUSH));
        }

        String ics = IcsExportService.build(plan, LocalDate.of(2026, 4, 20));

        assertThat(ics).startsWith("BEGIN:VCALENDAR\r\n");
        assertThat(ics).endsWith("END:VCALENDAR\r\n");
        long veventCount = ics.lines().filter(l -> l.equals("BEGIN:VEVENT")).count();
        assertThat(veventCount).isEqualTo(7);

        long rruleCount = ics.lines().filter(l -> l.startsWith("RRULE:FREQ=WEEKLY")).count();
        assertThat(rruleCount).isEqualTo(7);
    }

    @Test
    void emitsProperRruleByDayCodeForSunday() throws Exception {
        WorkoutPlan plan = plan("Sunday only");
        plan.getDays().add(day(plan, (short) 7, "Sunday", WorkoutFocus.REST));

        String ics = IcsExportService.build(plan, LocalDate.of(2026, 4, 20));

        assertThat(ics).contains("RRULE:FREQ=WEEKLY;BYDAY=SU");
    }

    @Test
    void escapesCommasInSummary() throws Exception {
        WorkoutPlan plan = plan("Plan, with comma");
        plan.getDays().add(day(plan, (short) 1, "Push, A", WorkoutFocus.PUSH));

        String ics = IcsExportService.build(plan, LocalDate.of(2026, 4, 20));
        assertThat(ics).contains("SUMMARY:Push\\, A");
    }

    private static WorkoutPlan plan(String name) throws Exception {
        WorkoutPlan p = new WorkoutPlan();
        setField(p, "id", UUID.randomUUID());
        p.setName(name);
        return p;
    }

    private static WorkoutDay day(
            WorkoutPlan plan, short dow, String name, WorkoutFocus focus) throws Exception {
        WorkoutDay d = new WorkoutDay();
        setField(d, "id", UUID.randomUUID());
        d.setPlan(plan);
        d.setDayOfWeek(dow);
        d.setName(name);
        d.setFocus(focus);
        return d;
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Class<?> c = target.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(fieldName);
                f.setAccessible(true);
                f.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}
