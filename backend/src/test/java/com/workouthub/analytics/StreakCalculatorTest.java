package com.workouthub.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class StreakCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 4, 23);

    @Test
    void returnsZeroForNoDates() {
        var r = StreakCalculator.compute(List.of(), TODAY);
        assertThat(r.current()).isZero();
        assertThat(r.longest()).isZero();
    }

    @Test
    void singleTodayIsOneOne() {
        var r = StreakCalculator.compute(List.of(TODAY), TODAY);
        assertThat(r.current()).isEqualTo(1);
        assertThat(r.longest()).isEqualTo(1);
    }

    @Test
    void singleYesterdayKeepsStreakAlive() {
        var r = StreakCalculator.compute(List.of(TODAY.minusDays(1)), TODAY);
        assertThat(r.current()).isEqualTo(1);
        assertThat(r.longest()).isEqualTo(1);
    }

    @Test
    void twoDaysAgoBreaksStreak() {
        var r = StreakCalculator.compute(List.of(TODAY.minusDays(2)), TODAY);
        assertThat(r.current()).isZero();
        assertThat(r.longest()).isEqualTo(1);
    }

    @Test
    void consecutiveThreeIncludingTodayIsThree() {
        var r = StreakCalculator.compute(
                List.of(TODAY.minusDays(2), TODAY.minusDays(1), TODAY), TODAY);
        assertThat(r.current()).isEqualTo(3);
        assertThat(r.longest()).isEqualTo(3);
    }

    @Test
    void brokenChainKeepsOldestLongest() {
        // A 4-day streak in March, broken, then a fresh 1-day today.
        var r = StreakCalculator.compute(
                List.of(
                        LocalDate.of(2026, 3, 1),
                        LocalDate.of(2026, 3, 2),
                        LocalDate.of(2026, 3, 3),
                        LocalDate.of(2026, 3, 4),
                        TODAY),
                TODAY);
        assertThat(r.current()).isEqualTo(1);
        assertThat(r.longest()).isEqualTo(4);
    }

    @Test
    void duplicateDatesAreDeduplicated() {
        var r = StreakCalculator.compute(
                List.of(TODAY.minusDays(1), TODAY.minusDays(1), TODAY), TODAY);
        assertThat(r.current()).isEqualTo(2);
        assertThat(r.longest()).isEqualTo(2);
    }

    @Test
    void streakThatEndsBeforeYesterdayIsZeroCurrent() {
        var r = StreakCalculator.compute(
                List.of(TODAY.minusDays(5), TODAY.minusDays(4), TODAY.minusDays(3)),
                TODAY);
        assertThat(r.current()).isZero();
        assertThat(r.longest()).isEqualTo(3);
    }
}
