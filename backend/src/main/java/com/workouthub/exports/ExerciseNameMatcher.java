package com.workouthub.exports;

import com.workouthub.exercises.domain.Exercise;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Maps a free-text exercise name from a third-party CSV export to a row
 * in our exercises catalog. Matching is tiered: exact, case-insensitive
 * trimmed, then substring in either direction. Callers get
 * Optional.empty() if no candidate passes any tier so they can surface
 * the unmatched rows to the user instead of silently dropping them.
 */
public final class ExerciseNameMatcher {

    private ExerciseNameMatcher() {}

    public static Optional<Exercise> match(String input, List<Exercise> catalog) {
        if (input == null || input.isBlank() || catalog == null || catalog.isEmpty()) {
            return Optional.empty();
        }
        String trimmed = input.trim();
        String normalized = trimmed.toLowerCase(Locale.ROOT);

        for (Exercise ex : catalog) {
            if (trimmed.equals(ex.getNameEn()) || trimmed.equals(ex.getNameTr())) {
                return Optional.of(ex);
            }
        }
        for (Exercise ex : catalog) {
            if (equalsIgnoreCase(normalized, ex.getNameEn())
                    || equalsIgnoreCase(normalized, ex.getNameTr())) {
                return Optional.of(ex);
            }
        }
        for (Exercise ex : catalog) {
            if (contains(normalized, ex.getNameEn())
                    || contains(normalized, ex.getNameTr())) {
                return Optional.of(ex);
            }
        }
        return Optional.empty();
    }

    private static boolean equalsIgnoreCase(String normalized, String candidate) {
        return candidate != null
                && normalized.equals(candidate.toLowerCase(Locale.ROOT));
    }

    private static boolean contains(String normalized, String candidate) {
        if (candidate == null || candidate.isBlank()) return false;
        String c = candidate.toLowerCase(Locale.ROOT);
        return normalized.contains(c) || c.contains(normalized);
    }
}
