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

    @Test
    void freezeKeepsStreakWhenSingleMidGapIsAllowed() {
        // ...3 ago, 2 ago skipped, 1 ago, today. Without freeze: would break
        // at the 2-day gap. With freeze: continues and consumes the freeze.
        var r = StreakCalculator.computeWithFreeze(
                List.of(TODAY.minusDays(3), TODAY.minusDays(1), TODAY),
                TODAY,
                true);
        assertThat(r.current()).isEqualTo(3);
        assertThat(r.freezeUsedInRun()).isTrue();
    }

    @Test
    void streakBreaksAtSecondGapEvenWhenFreezeIsAvailable() {
        // Walk-back from today: consecutive through -1 -> -2, freeze covers
        // the -2 -> -4 jump (2-day gap), continues -4 -> -5 (consecutive),
        // then stops at -5 -> -7 (a 2-day gap with no freeze left).
        var r = StreakCalculator.computeWithFreeze(
                List.of(
                        TODAY.minusDays(7),
                        TODAY.minusDays(5),
                        TODAY.minusDays(4),
                        TODAY.minusDays(2),
                        TODAY.minusDays(1),
                        TODAY),
                TODAY,
                true);
        assertThat(r.current()).isEqualTo(5);
        assertThat(r.freezeUsedInRun()).isTrue();
    }

    @Test
    void streakDoesNotSurviveSecondGapWhenSeparated() {
        // -7 -> -5 is a 2-day gap with no freeze left; the walk stops there.
        // Run includes today, -1, -3 (after freeze), -4, -5 - five dates.
        var r = StreakCalculator.computeWithFreeze(
                List.of(
                        TODAY.minusDays(7),
                        TODAY.minusDays(5),
                        TODAY.minusDays(4),
                        TODAY.minusDays(3),
                        TODAY.minusDays(1),
                        TODAY),
                TODAY,
                true);
        assertThat(r.current()).isEqualTo(5);
        assertThat(r.freezeUsedInRun()).isTrue();
    }

    @Test
    void freezeNotUsedWhenStreakIsAlreadyConsecutive() {
        var r = StreakCalculator.computeWithFreeze(
                List.of(TODAY.minusDays(2), TODAY.minusDays(1), TODAY),
                TODAY,
                true);
        assertThat(r.current()).isEqualTo(3);
        assertThat(r.freezeUsedInRun()).isFalse();
    }
}
