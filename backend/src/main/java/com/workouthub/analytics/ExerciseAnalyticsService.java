package com.workouthub.analytics;

import com.workouthub.analytics.dto.LastPerformanceDto;
import com.workouthub.analytics.dto.ProgressPointDto;
import com.workouthub.common.web.NotFoundException;
import com.workouthub.exercises.domain.ExerciseRepository;
import com.workouthub.sessions.SessionsMapper;
import com.workouthub.sessions.domain.SessionSet;
import com.workouthub.sessions.domain.SessionSetRepository;
import com.workouthub.sessions.domain.WorkoutSession;
import com.workouthub.sessions.dto.SessionSetDto;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ExerciseAnalyticsService {

    private final SessionSetRepository sets;
    private final ExerciseRepository exercises;

    public ExerciseAnalyticsService(SessionSetRepository sets, ExerciseRepository exercises) {
        this.sets = sets;
        this.exercises = exercises;
    }

    public Optional<LastPerformanceDto> lastPerformance(UUID userId, UUID exerciseId) {
        exercises.findById(exerciseId)
                .orElseThrow(() -> new NotFoundException("Exercise not found: " + exerciseId));
        List<SessionSet> history = sets.findHistoricalByUserAndExercise(userId, exerciseId);
        if (history.isEmpty()) {
            return Optional.empty();
        }
        WorkoutSession mostRecent = history.get(0).getSession();
        List<SessionSetDto> bundle = history.stream()
                .takeWhile(s -> s.getSession().getId().equals(mostRecent.getId()))
                .sorted(Comparator.comparing(SessionSet::getSetNumber))
                .map(SessionsMapper::toSetDto)
                .toList();
        return Optional.of(new LastPerformanceDto(
                mostRecent.getId(),
                mostRecent.getStartedAt(),
                mostRecent.getEndedAt(),
                bundle));
    }

    public List<ProgressPointDto> progress(UUID userId, UUID exerciseId, int limit) {
        exercises.findById(exerciseId)
                .orElseThrow(() -> new NotFoundException("Exercise not found: " + exerciseId));
        List<SessionSet> history = sets.findHistoricalByUserAndExercise(userId, exerciseId);

        Map<UUID, List<SessionSet>> bySession = new LinkedHashMap<>();
        for (SessionSet s : history) {
            bySession.computeIfAbsent(s.getSession().getId(), k -> new ArrayList<>()).add(s);
        }

        return bySession.values().stream()
                .limit(limit)
                .map(ExerciseAnalyticsService::summarize)
                .toList();
    }

    private static ProgressPointDto summarize(List<SessionSet> setsInSession) {
        WorkoutSession session = setsInSession.get(0).getSession();
        BigDecimal totalVolume = setsInSession.stream()
                .map(s -> (s.getWeightKg() == null ? BigDecimal.ZERO : s.getWeightKg())
                        .multiply(BigDecimal.valueOf(s.getRepsDone())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal maxWeight = setsInSession.stream()
                .map(SessionSet::getWeightKg)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
        short topReps = (short) setsInSession.stream()
                .mapToInt(SessionSet::getRepsDone)
                .max()
                .orElse(0);
        // Per-top-set Epley: max(epleyOneRm(weight, reps)) across each set, not epleyOneRm(maxWeight, topReps),
        // because maxWeight and topReps may belong to different sets and would yield a non-physical 1RM.
        BigDecimal estimatedOneRm = setsInSession.stream()
                .map(s -> PrDetector.epleyOneRm(s.getWeightKg(), s.getRepsDone()))
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
        return new ProgressPointDto(
                session.getId(),
                session.getStartedAt(),
                setsInSession.size(),
                totalVolume,
                maxWeight,
                topReps,
                estimatedOneRm);
    }
}
