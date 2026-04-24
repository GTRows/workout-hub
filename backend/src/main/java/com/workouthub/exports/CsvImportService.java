package com.workouthub.exports;

import com.workouthub.exercises.domain.Exercise;
import com.workouthub.exercises.domain.ExerciseRepository;
import com.workouthub.exports.dto.CsvImportResultDto;
import com.workouthub.sessions.domain.SessionSet;
import com.workouthub.sessions.domain.WorkoutSession;
import com.workouthub.sessions.domain.WorkoutSessionRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Imports a Strong/Hevy-style CSV back into the stack. Rows are grouped
 * into sessions by (Date, Workout Name); unmatched exercise names are
 * collected and surfaced in the response instead of failing the import.
 */
@Service
@Transactional
public class CsvImportService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
    private static final ZoneId ZONE = ZoneId.systemDefault();

    private final ExerciseRepository exercises;
    private final WorkoutSessionRepository sessions;

    public CsvImportService(
            ExerciseRepository exercises,
            WorkoutSessionRepository sessions) {
        this.exercises = exercises;
        this.sessions = sessions;
    }

    public CsvImportResultDto importSessionsCsv(UUID userId, String csv) {
        List<Exercise> catalog = exercises.findAll();
        List<String[]> rows = CsvRowReader.readAll(csv);
        if (rows.isEmpty()) {
            return new CsvImportResultDto(0, 0, List.of(), List.of());
        }

        Map<Integer, String> header = indexHeader(rows.get(0));
        List<Row> parsed = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (int i = 1; i < rows.size(); i++) {
            String[] raw = rows.get(i);
            Row r = parseRow(header, raw, i + 1, warnings);
            if (r != null) parsed.add(r);
        }

        Map<String, List<Row>> grouped = new LinkedHashMap<>();
        for (Row r : parsed) {
            grouped.computeIfAbsent(r.groupKey(), k -> new ArrayList<>()).add(r);
        }

        TreeSet<String> unmatched = new TreeSet<>();
        int sessionsInserted = 0;
        int setsInserted = 0;

        for (Map.Entry<String, List<Row>> entry : grouped.entrySet()) {
            List<Row> group = entry.getValue();
            Row first = group.get(0);
            WorkoutSession session = new WorkoutSession();
            session.setUserId(userId);
            session.setStartedAt(first.date.atZone(ZONE).toInstant());
            session.setEndedAt(first.date.atZone(ZONE).toInstant());
            session.setNotes(first.workoutNotes);

            short setNumber = 0;
            for (Row r : group) {
                Optional<Exercise> match =
                        ExerciseNameMatcher.match(r.exerciseName, catalog);
                if (match.isEmpty()) {
                    unmatched.add(r.exerciseName);
                    continue;
                }
                SessionSet set = new SessionSet();
                set.setExercise(match.get());
                set.setSetNumber(r.setOrder != null ? r.setOrder : ++setNumber);
                set.setRepsDone(r.reps);
                set.setWeightKg(r.weight);
                set.setCompleted(true);
                set.setNotes(r.notes);
                session.addSet(set);
                setsInserted++;
            }

            if (!session.getSets().isEmpty()) {
                sessions.save(session);
                sessionsInserted++;
            }
        }

        return new CsvImportResultDto(
                sessionsInserted, setsInserted, new ArrayList<>(unmatched), warnings);
    }

    private static Map<Integer, String> indexHeader(String[] headerRow) {
        Map<Integer, String> out = new LinkedHashMap<>();
        for (int i = 0; i < headerRow.length; i++) {
            out.put(i, headerRow[i].trim().toLowerCase(Locale.ROOT));
        }
        return out;
    }

    private static Row parseRow(
            Map<Integer, String> header, String[] raw, int lineNo, List<String> warnings) {
        try {
            Row r = new Row();
            for (int i = 0; i < raw.length; i++) {
                String column = header.get(i);
                String value = raw[i];
                if (column == null || value == null) continue;
                switch (column) {
                    case "date" -> r.date = LocalDateTime.parse(value, DATE_FMT);
                    case "workout name" -> r.workoutName = value;
                    case "exercise name" -> r.exerciseName = value;
                    case "set order" -> r.setOrder = shortOrNull(value);
                    case "weight" -> r.weight = bigDecimalOrNull(value);
                    case "reps" -> r.reps = (short) Math.max(0,
                            Integer.parseInt(value.isBlank() ? "0" : value));
                    case "notes" -> r.notes = blankToNull(value);
                    case "workout notes" -> r.workoutNotes = blankToNull(value);
                    default -> {
                        // ignore unknown columns silently to stay Strong/Hevy-flexible
                    }
                }
            }
            if (r.date == null || r.exerciseName == null || r.exerciseName.isBlank()) {
                warnings.add("line " + lineNo + " skipped: missing date or exercise name");
                return null;
            }
            return r;
        } catch (Exception ex) {
            warnings.add("line " + lineNo + " skipped: " + ex.getMessage());
            return null;
        }
    }

    private static Short shortOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Short.parseShort(s); } catch (NumberFormatException e) { return null; }
    }

    private static BigDecimal bigDecimalOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try { return new BigDecimal(s); } catch (NumberFormatException e) { return null; }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static final class Row {
        LocalDateTime date;
        String workoutName;
        String exerciseName;
        Short setOrder;
        BigDecimal weight;
        short reps;
        String notes;
        String workoutNotes;

        String groupKey() {
            return date + "|" + (workoutName == null ? "" : workoutName);
        }
    }
}
