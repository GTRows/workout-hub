package com.workouthub.sessions;

import com.workouthub.achievements.AchievementEvaluator;
import com.workouthub.analytics.PrDetector;
import com.workouthub.common.web.ConflictException;
import com.workouthub.common.web.NotFoundException;
import com.workouthub.exercises.domain.Exercise;
import com.workouthub.exercises.domain.ExerciseRepository;
import com.workouthub.sessions.domain.SessionSet;
import com.workouthub.sessions.domain.SessionSetRepository;
import com.workouthub.sessions.domain.WorkoutSession;
import com.workouthub.sessions.dto.AddSetRequest;
import com.workouthub.sessions.dto.SessionSetDto;
import com.workouthub.sessions.dto.UpdateSetRequest;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SessionSetsService {

    public record AddSetResult(SessionSetDto dto, boolean idempotentHit) {}

    private static final String CODE_SET_NUMBER_DUPLICATE = "SET_NUMBER_DUPLICATE";

    private final SessionsService sessionsService;
    private final SessionSetRepository sets;
    private final ExerciseRepository exercises;
    private final AchievementEvaluator achievements;

    public SessionSetsService(
            SessionsService sessionsService,
            SessionSetRepository sets,
            ExerciseRepository exercises,
            AchievementEvaluator achievements) {
        this.sessionsService = sessionsService;
        this.sets = sets;
        this.exercises = exercises;
        this.achievements = achievements;
    }

    /**
     * Adds a set to an active session.
     *
     * <p>Auto-numbering contract: when {@code req.setNumber()} is null, the server
     * assigns {@code countBySessionIdAndExerciseId + 1}. This is safe ONLY for the
     * live-online "next set" UX where a single client adds sets sequentially.
     *
     * <p>Offline-queue drainers MUST send an explicit {@code setNumber} on every
     * POST. Out-of-order drain (e.g. sets {1, 3, 2}) without explicit numbers
     * corrupts row order because each implicit insert sees a different in-flight
     * count. The integration test
     * {@code SessionSetsIntegrationTest.outOfOrderDrainPreservesSetNumberOrdering}
     * proves the explicit-number path tolerates out-of-order drain.
     *
     * <p>Idempotency contract: when {@code req.clientSetId()} is non-null and a
     * row already exists for {@code (sessionId, clientSetId)}, the persisted row
     * is returned with {@code idempotentHit=true}. The PR detector and
     * achievement evaluator are not re-fired on a replay.
     */
    public AddSetResult add(UUID userId, UUID sessionId, AddSetRequest req) {
        WorkoutSession session = sessionsService.findActiveOwnedOrThrow(userId, sessionId);
        Exercise exercise = exercises.findById(req.exerciseId())
                .orElseThrow(() -> new NotFoundException(
                        "Exercise not found: " + req.exerciseId()));

        if (req.clientSetId() != null) {
            var existing = sets.findBySessionIdAndClientSetId(sessionId, req.clientSetId());
            if (existing.isPresent()) {
                return new AddSetResult(SessionsMapper.toSetDto(existing.get()), true);
            }
        }

        short setNumber = req.setNumber() != null
                ? req.setNumber()
                : (short) (sets.countBySessionIdAndExerciseId(sessionId, exercise.getId()) + 1);

        BigDecimal priorBest = bestPriorOneRm(userId, exercise.getId());
        boolean isPr = req.completed() != Boolean.FALSE
                && PrDetector.beatsPriorBest(priorBest, req.weightKg(), req.repsDone());

        SessionSet set = new SessionSet();
        set.setExercise(exercise);
        set.setSetNumber(setNumber);
        set.setRepsDone(req.repsDone());
        set.setWeightKg(req.weightKg());
        set.setRpe(req.rpe());
        set.setCompleted(req.completed() == null ? Boolean.TRUE : req.completed());
        set.setNotes(req.notes());
        set.setClientSetId(req.clientSetId());
        set.setPr(isPr);
        session.addSet(set);
        try {
            SessionSet saved = sets.saveAndFlush(set);
            achievements.onSetSaved(userId, isPr);
            return new AddSetResult(SessionsMapper.toSetDto(saved), false);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new ConflictException(
                    CODE_SET_NUMBER_DUPLICATE,
                    "Set number " + setNumber + " already recorded for this exercise");
        }
    }

    private BigDecimal bestPriorOneRm(UUID userId, UUID exerciseId) {
        List<SessionSet> history = sets.findHistoricalByUserAndExercise(userId, exerciseId);
        BigDecimal best = null;
        for (SessionSet s : history) {
            if (!s.isCompleted()) continue;
            BigDecimal oneRm = PrDetector.epleyOneRm(s.getWeightKg(), s.getRepsDone());
            if (oneRm == null) continue;
            if (best == null || oneRm.compareTo(best) > 0) best = oneRm;
        }
        return best;
    }

    public SessionSetDto update(UUID userId, UUID sessionId, UUID setId, UpdateSetRequest req) {
        // Gate: session exists, belongs to user, still open.
        sessionsService.findActiveOwnedOrThrow(userId, sessionId);
        SessionSet set = sets.findByIdAndSessionId(setId, sessionId)
                .orElseThrow(() -> new NotFoundException("Session set not found: " + setId));

        if (req.repsDone() != null) set.setRepsDone(req.repsDone());
        if (req.weightKg() != null) set.setWeightKg(req.weightKg());
        if (req.rpe() != null) set.setRpe(req.rpe());
        if (req.completed() != null) set.setCompleted(req.completed());
        if (req.notes() != null) set.setNotes(req.notes());
        recomputePrForExerciseHistory(userId, set.getExercise().getId());
        return SessionsMapper.toSetDto(set);
    }

    /**
     * Demotes-and-re-elects the durable PR row for a (user, exercise) pair.
     *
     * <p>Scans every set the user has performed for the exercise (including
     * sets in active sessions, because PRs span finished and in-progress data),
     * picks the completed set with the highest Epley one-rep-max, and writes
     * {@code is_pr=true} on that single row while clearing every other row.
     * O(n) where n is bounded by single-user history; PUT /sets/:setId is rare.
     *
     * <p>The {@code Comparator.nullsFirst} guard ensures unweighted sets (null
     * Epley) never outrank weighted sets via {@code max}. If every set has a
     * null Epley, no row is promoted and all rows are cleared.
     */
    private void recomputePrForExerciseHistory(UUID userId, UUID exerciseId) {
        List<SessionSet> history = sets.findAllByUserAndExercise(userId, exerciseId);
        Optional<SessionSet> winner = history.stream()
                .filter(SessionSet::isCompleted)
                .filter(s -> PrDetector.epleyOneRm(s.getWeightKg(), s.getRepsDone()) != null)
                .max(Comparator.comparing(
                        s -> PrDetector.epleyOneRm(s.getWeightKg(), s.getRepsDone()),
                        Comparator.nullsFirst(Comparator.naturalOrder())));
        UUID winnerId = winner.map(SessionSet::getId).orElse(null);
        for (SessionSet s : history) {
            s.setPr(winnerId != null && s.getId().equals(winnerId));
        }
    }

    public void delete(UUID userId, UUID sessionId, UUID setId) {
        WorkoutSession session = sessionsService.findActiveOwnedOrThrow(userId, sessionId);
        SessionSet set = sets.findByIdAndSessionId(setId, sessionId)
                .orElseThrow(() -> new NotFoundException("Session set not found: " + setId));
        session.removeSet(set);
    }
}
