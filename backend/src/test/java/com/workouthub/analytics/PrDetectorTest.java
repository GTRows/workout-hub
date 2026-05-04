package com.workouthub.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PrDetectorTest {

    @Test
    void epleyHandlesOneRepAsIdentity() {
        BigDecimal r = PrDetector.epleyOneRm(new BigDecimal("100"), (short) 1);
        assertThat(r).isEqualByComparingTo("100");
    }

    @Test
    void epleyApproxEighty() {
        // 60 * (1 + 10/30) = 60 * 1.333333 = 80.000
        BigDecimal r = PrDetector.epleyOneRm(new BigDecimal("60"), (short) 10);
        assertThat(r).isEqualByComparingTo("80.000");
    }

    @Test
    void epleyReturnsNullWhenWeightMissing() {
        assertThat(PrDetector.epleyOneRm(null, (short) 5)).isNull();
    }

    @Test
    void epleyReturnsNullForZeroReps() {
        assertThat(PrDetector.epleyOneRm(new BigDecimal("100"), (short) 0)).isNull();
    }

    @Test
    void firstCompletedSetIsAlwaysAPr() {
        assertThat(PrDetector.beatsPriorBest(null, new BigDecimal("50"), (short) 5)).isTrue();
    }

    @Test
    void matchingPriorBestIsNotAPr() {
        BigDecimal prior = PrDetector.epleyOneRm(new BigDecimal("100"), (short) 5);
        assertThat(PrDetector.beatsPriorBest(prior, new BigDecimal("100"), (short) 5)).isFalse();
    }

    @Test
    void higherWeightSameRepsBeatsPrior() {
        BigDecimal prior = PrDetector.epleyOneRm(new BigDecimal("100"), (short) 5);
        assertThat(PrDetector.beatsPriorBest(prior, new BigDecimal("102.5"), (short) 5)).isTrue();
    }

    @Test
    void sameWeightMoreRepsBeatsPrior() {
        BigDecimal prior = PrDetector.epleyOneRm(new BigDecimal("100"), (short) 5);
        assertThat(PrDetector.beatsPriorBest(prior, new BigDecimal("100"), (short) 6)).isTrue();
    }

    @Test
    void missingWeightIsNeverAPr() {
        assertThat(PrDetector.beatsPriorBest(null, null, (short) 10)).isFalse();
    }
}
