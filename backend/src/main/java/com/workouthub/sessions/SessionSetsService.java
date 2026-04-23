package com.workouthub.sessions;

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
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SessionSetsService {

    private final SessionsService sessionsService;
    private final SessionSetRepository sets;
    private final ExerciseRepository exercises;

    public SessionSetsService(
            SessionsService sessionsService,
            SessionSetRepository sets,
            ExerciseRepository exercises) {
        this.sessionsService = sessionsService;
        this.sets = sets;
        this.exercises = exercises;
    }

    public SessionSetDto add(UUID userId, UUID sessionId, AddSetRequest req) {
        WorkoutSession session = sessionsService.findActiveOwnedOrThrow(userId, sessionId);
        Exercise exercise = exercises.findById(req.exerciseId())
                .orElseThrow(() -> new NotFoundException(
                        "Exercise not found: " + req.exerciseId()));

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
        session.addSet(set);
        try {
            SessionSet saved = sets.saveAndFlush(set);
            return SessionsMapper.toSetDto(saved, isPr ? Boolean.TRUE : null);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new ConflictException(
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
        return SessionsMapper.toSetDto(set);
    }

    public void delete(UUID userId, UUID sessionId, UUID setId) {
        WorkoutSession session = sessionsService.findActiveOwnedOrThrow(userId, sessionId);
        SessionSet set = sets.findByIdAndSessionId(setId, sessionId)
                .orElseThrow(() -> new NotFoundException("Session set not found: " + setId));
        session.removeSet(set);
    }
}
