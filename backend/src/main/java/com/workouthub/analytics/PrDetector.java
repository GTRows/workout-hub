package com.workouthub.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PrDetector {

    private static final BigDecimal REP_DENOM = BigDecimal.valueOf(30);

    private PrDetector() {}

    public static BigDecimal epleyOneRm(BigDecimal weightKg, short reps) {
        if (weightKg == null || reps <= 0) return null;
        if (reps == 1) return weightKg;
        BigDecimal factor = BigDecimal.ONE.add(
                BigDecimal.valueOf(reps).divide(REP_DENOM, 6, RoundingMode.HALF_UP));
        return weightKg.multiply(factor).setScale(3, RoundingMode.HALF_UP);
    }

    public static boolean beatsPriorBest(
            BigDecimal priorBest, BigDecimal candidateWeight, short candidateReps) {
        BigDecimal candidate = epleyOneRm(candidateWeight, candidateReps);
        if (candidate == null) return false;
        if (priorBest == null) return true;
        return candidate.compareTo(priorBest) > 0;
    }
}
