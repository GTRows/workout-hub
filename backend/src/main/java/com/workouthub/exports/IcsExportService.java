package com.workouthub.exports;

import com.workouthub.common.web.NotFoundException;
import com.workouthub.workouts.domain.WorkoutDay;
import com.workouthub.workouts.domain.WorkoutPlan;
import com.workouthub.workouts.domain.WorkoutPlanRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Emits the active plan as an RFC 5545 iCalendar feed with one weekly-
 * recurring VEVENT per day in the plan.
 */
@Service
@Transactional(readOnly = true)
public class IcsExportService {

    private static final DateTimeFormatter ICS_UTC =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final LocalTime DEFAULT_START = LocalTime.of(18, 0);
    private static final int DEFAULT_DURATION_MIN = 60;

    private final WorkoutPlanRepository plans;

    public IcsExportService(WorkoutPlanRepository plans) {
        this.plans = plans;
    }

    public String buildActivePlanIcs(UUID userId) {
        WorkoutPlan plan = plans.findByUserIdAndActiveTrue(userId)
                .orElseThrow(() -> new NotFoundException(
                        "No active plan for user " + userId));
        return build(plan, LocalDate.now());
    }

    static String build(WorkoutPlan plan, LocalDate referenceDate) {
        LocalDateTime stamp = LocalDateTime.now(ZoneOffset.UTC);
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n")
                .append("VERSION:2.0\r\n")
                .append("PRODID:-//WorkoutHub//Plan Export//EN\r\n")
                .append("CALSCALE:GREGORIAN\r\n");

        for (WorkoutDay d : plan.getDays()) {
            DayOfWeek dow = DayOfWeek.of(d.getDayOfWeek());
            LocalDate firstOccurrence = referenceDate.with(TemporalAdjusters.nextOrSame(dow));
            int durationMin = d.getEstimatedDurationMin() == null
                    ? DEFAULT_DURATION_MIN
                    : d.getEstimatedDurationMin();
            LocalDateTime start = LocalDateTime.of(firstOccurrence, DEFAULT_START);
            LocalDateTime end = start.plusMinutes(durationMin);

            sb.append("BEGIN:VEVENT\r\n")
                    .append("UID:").append(d.getId()).append("@workouthub\r\n")
                    .append("DTSTAMP:").append(ICS_UTC.format(stamp)).append("\r\n")
                    .append("DTSTART:").append(ICS_UTC.format(start)).append("\r\n")
                    .append("DTEND:").append(ICS_UTC.format(end)).append("\r\n")
                    .append("SUMMARY:").append(escape(d.getName() == null
                            ? plan.getName() : d.getName())).append("\r\n")
                    .append("DESCRIPTION:")
                    .append(escape("WorkoutHub plan: " + plan.getName()))
                    .append("\r\n")
                    .append("RRULE:FREQ=WEEKLY;BYDAY=").append(byDay(dow)).append("\r\n")
                    .append("END:VEVENT\r\n");
        }

        sb.append("END:VCALENDAR\r\n");
        return sb.toString();
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace(",", "\\,")
                .replace(";", "\\;")
                .replace("\n", "\\n");
    }

    private static String byDay(DayOfWeek dow) {
        return switch (dow) {
            case MONDAY -> "MO";
            case TUESDAY -> "TU";
            case WEDNESDAY -> "WE";
            case THURSDAY -> "TH";
            case FRIDAY -> "FR";
            case SATURDAY -> "SA";
            case SUNDAY -> "SU";
        };
    }
}
