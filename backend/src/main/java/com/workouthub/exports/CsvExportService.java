package com.workouthub.exports;

import com.workouthub.sessions.domain.SessionSet;
import com.workouthub.sessions.domain.WorkoutSession;
import com.workouthub.sessions.domain.WorkoutSessionRepository;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Strong/Hevy-compatible CSV export for session history. Column order
 * and header match the Strong app export format closely enough that the
 * result can be re-imported by most third-party workout trackers.
 */
@Service
@Transactional(readOnly = true)
public class CsvExportService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId ZONE = ZoneId.systemDefault();
    static final String HEADER =
            "Date,Workout Name,Exercise Name,Set Order,Weight,Reps,Notes,Workout Notes";

    private final WorkoutSessionRepository sessions;

    public CsvExportService(WorkoutSessionRepository sessions) {
        this.sessions = sessions;
    }

    public String buildSessionsCsv(UUID userId) {
        List<WorkoutSession> history = sessions
                .findByUserIdAndEndedAtIsNotNullOrderByStartedAtDesc(userId);
        return buildCsv(history);
    }

    static String buildCsv(List<WorkoutSession> history) {
        StringBuilder sb = new StringBuilder();
        sb.append(HEADER).append('\n');
        for (WorkoutSession s : history) {
            String date = DATE_FMT.format(s.getStartedAt().atZone(ZONE));
            String workoutName = "Workout " + s.getId();
            String workoutNotes = csvField(s.getNotes());
            for (SessionSet set : s.getSets()) {
                String exercise = set.getExercise() == null
                        ? ""
                        : set.getExercise().getNameEn();
                String weight = set.getWeightKg() == null
                        ? ""
                        : stripTrailingZeros(set.getWeightKg());
                sb.append(csvField(date)).append(',')
                        .append(csvField(workoutName)).append(',')
                        .append(csvField(exercise)).append(',')
                        .append(set.getSetNumber()).append(',')
                        .append(weight).append(',')
                        .append(set.getRepsDone()).append(',')
                        .append(csvField(set.getNotes())).append(',')
                        .append(workoutNotes).append('\n');
            }
        }
        return sb.toString();
    }

    private static String csvField(String value) {
        if (value == null) return "";
        boolean needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n");
        if (!needsQuotes) return value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static String stripTrailingZeros(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
