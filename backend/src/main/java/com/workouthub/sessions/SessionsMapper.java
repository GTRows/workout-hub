package com.workouthub.sessions;

import com.workouthub.sessions.domain.SessionSet;
import com.workouthub.sessions.domain.WorkoutSession;
import com.workouthub.sessions.dto.SessionDto;
import com.workouthub.sessions.dto.SessionSetDto;
import com.workouthub.sessions.dto.SessionSummaryDto;
import java.util.Comparator;

public final class SessionsMapper {

    private SessionsMapper() {}

    public static SessionDto toDto(WorkoutSession s) {
        var sets = s.getSets().stream()
                .sorted(Comparator
                        .comparing((SessionSet x) -> x.getExercise().getId())
                        .thenComparingInt(SessionSet::getSetNumber))
                .map(SessionsMapper::toSetDto)
                .toList();
        return new SessionDto(
                s.getId(),
                s.getWorkoutDayId(),
                s.getStartedAt(),
                s.getEndedAt(),
                s.getNotes(),
                s.getMood(),
                s.getEnergyLevel(),
                s.isFinished(),
                sets,
                s.getHeartRateAvgBpm());
    }

    public static SessionSummaryDto toSummary(WorkoutSession s) {
        return new SessionSummaryDto(
                s.getId(),
                s.getWorkoutDayId(),
                s.getStartedAt(),
                s.getEndedAt(),
                s.isFinished(),
                s.getSets() == null ? 0 : s.getSets().size(),
                s.getMood(),
                s.getEnergyLevel(),
                s.getHeartRateAvgBpm());
    }

    public static SessionSetDto toSetDto(SessionSet set) {
        return toSetDto(set, null);
    }

    public static SessionSetDto toSetDto(SessionSet set, Boolean newPr) {
        var exercise = set.getExercise();
        return new SessionSetDto(
                set.getId(),
                exercise == null ? null : exercise.getId(),
                exercise == null ? null : exercise.getNameTr(),
                exercise == null ? null : exercise.getNameEn(),
                set.getSetNumber(),
                set.getRepsDone(),
                set.getWeightKg(),
                set.getRpe(),
                set.isCompleted(),
                set.getNotes(),
                newPr);
    }
}
