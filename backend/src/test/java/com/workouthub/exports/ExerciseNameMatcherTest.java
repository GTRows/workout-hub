package com.workouthub.exports;

import static org.assertj.core.api.Assertions.assertThat;

import com.workouthub.exercises.domain.Exercise;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExerciseNameMatcherTest {

    @Test
    void returnsEmptyForNullOrBlankInput() {
        Exercise bench = exercise("Bench Press", "Bench Press");
        assertThat(ExerciseNameMatcher.match(null, List.of(bench))).isEmpty();
        assertThat(ExerciseNameMatcher.match("", List.of(bench))).isEmpty();
        assertThat(ExerciseNameMatcher.match("   ", List.of(bench))).isEmpty();
    }

    @Test
    void exactMatchWinsOverSubstring() {
        Exercise bench = exercise("Bench Press", "Bench Press");
        Exercise benchNarrow = exercise("Narrow Bench Press", "Narrow Bench Press");
        Optional<Exercise> hit =
                ExerciseNameMatcher.match("Bench Press", List.of(benchNarrow, bench));
        assertThat(hit).contains(bench);
    }

    @Test
    void caseInsensitiveMatchWorks() {
        Exercise bench = exercise("Bench Press", "Bench Press");
        assertThat(ExerciseNameMatcher.match("bench press", List.of(bench))).contains(bench);
        assertThat(ExerciseNameMatcher.match("BENCH PRESS", List.of(bench))).contains(bench);
    }

    @Test
    void substringMatchIsBidirectional() {
        Exercise squat = exercise("Back Squat", "Back Squat");
        Exercise deadlift = exercise("Deadlift", "Deadlift");

        // input narrower than catalog entry
        assertThat(ExerciseNameMatcher.match("Squat", List.of(squat, deadlift))).contains(squat);
        // input wider than catalog entry
        assertThat(ExerciseNameMatcher.match("Back Squat (High Bar)", List.of(squat, deadlift)))
                .contains(squat);
    }

    @Test
    void returnsEmptyWhenNoTierMatches() {
        Exercise bench = exercise("Bench Press", "Bench Press");
        assertThat(ExerciseNameMatcher.match("Zercher Carry", List.of(bench))).isEmpty();
    }

    @Test
    void matchesTurkishCatalogName() {
        Exercise ex = exercise("Goguse Dumbell Press", "Chest Dumbbell Press");
        assertThat(ExerciseNameMatcher.match("Goguse Dumbell Press", List.of(ex))).contains(ex);
    }

    private static Exercise exercise(String tr, String en) {
        Exercise e = new Exercise();
        setField(e, "id", UUID.randomUUID());
        setField(e, "nameTr", tr);
        setField(e, "nameEn", en);
        return e;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field f = Exercise.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }
}
