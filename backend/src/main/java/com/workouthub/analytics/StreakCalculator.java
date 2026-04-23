package com.workouthub.analytics;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

public final class StreakCalculator {

    public record Result(int current, int longest) {}

    private StreakCalculator() {}

    public static Result compute(Collection<LocalDate> rawSessionDates, LocalDate today) {
        if (rawSessionDates == null || rawSessionDates.isEmpty()) {
            return new Result(0, 0);
        }

        List<LocalDate> dates = new TreeSet<>(rawSessionDates).stream().toList();

        int longest = 1;
        int run = 1;
        for (int i = 1; i < dates.size(); i++) {
            if (dates.get(i).minusDays(1).equals(dates.get(i - 1))) {
                run++;
                longest = Math.max(longest, run);
            } else {
                run = 1;
            }
        }

        LocalDate last = dates.get(dates.size() - 1);
        int current;
        if (last.equals(today) || last.equals(today.minusDays(1))) {
            current = 1;
            for (int i = dates.size() - 2; i >= 0; i--) {
                if (dates.get(i + 1).minusDays(1).equals(dates.get(i))) {
                    current++;
                } else {
                    break;
                }
            }
        } else {
            current = 0;
        }

        return new Result(current, longest);
    }
}
