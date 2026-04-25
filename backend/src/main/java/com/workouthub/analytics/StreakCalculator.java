package com.workouthub.analytics;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

public final class StreakCalculator {

    public record Result(int current, int longest) {}

    public record FreezeAwareResult(int current, int longest, boolean freezeUsedInRun) {}

    private StreakCalculator() {}

    public static Result compute(Collection<LocalDate> rawSessionDates, LocalDate today) {
        FreezeAwareResult r = computeWithFreeze(rawSessionDates, today, false);
        return new Result(r.current(), r.longest());
    }

    public static FreezeAwareResult computeWithFreeze(
            Collection<LocalDate> rawSessionDates,
            LocalDate today,
            boolean freezeAvailable) {
        if (rawSessionDates == null || rawSessionDates.isEmpty()) {
            return new FreezeAwareResult(0, 0, false);
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
        boolean freezeUsed = false;
        boolean freezeRemaining = freezeAvailable;
        boolean lastDateMatchesToday =
                last.equals(today) || last.equals(today.minusDays(1));
        boolean lastDateOneFreezeAway =
                freezeRemaining && last.equals(today.minusDays(2));

        if (lastDateMatchesToday) {
            current = 1;
        } else if (lastDateOneFreezeAway) {
            current = 1;
            freezeUsed = true;
            freezeRemaining = false;
        } else {
            return new FreezeAwareResult(0, longest, false);
        }

        for (int i = dates.size() - 2; i >= 0; i--) {
            LocalDate next = dates.get(i + 1);
            LocalDate cur = dates.get(i);
            if (next.minusDays(1).equals(cur)) {
                current++;
            } else if (freezeRemaining && next.minusDays(2).equals(cur)) {
                current++;
                freezeUsed = true;
                freezeRemaining = false;
            } else {
                break;
            }
        }

        return new FreezeAwareResult(current, longest, freezeUsed);
    }
}
