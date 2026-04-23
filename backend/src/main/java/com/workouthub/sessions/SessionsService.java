package com.workouthub.sessions;

import com.workouthub.common.web.ConflictException;
import com.workouthub.common.web.NotFoundException;
import com.workouthub.sessions.domain.WorkoutSession;
import com.workouthub.sessions.domain.WorkoutSessionRepository;
import com.workouthub.sessions.dto.FinishSessionRequest;
import com.workouthub.sessions.dto.SessionDto;
import com.workouthub.sessions.dto.StartSessionRequest;
import com.workouthub.workouts.domain.WorkoutDayRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SessionsService {

    private final WorkoutSessionRepository sessions;
    private final WorkoutDayRepository days;

    public SessionsService(WorkoutSessionRepository sessions, WorkoutDayRepository days) {
        this.sessions = sessions;
        this.days = days;
    }

    public SessionDto start(UUID userId, StartSessionRequest req) {
        if (sessions.findByUserIdAndEndedAtIsNull(userId).isPresent()) {
            throw new ConflictException("You already have an active session");
        }
        WorkoutSession session = new WorkoutSession();
        session.setUserId(userId);
        if (req != null && req.workoutDayId() != null) {
            days.findByIdAndPlanUserId(req.workoutDayId(), userId)
                    .orElseThrow(() -> new NotFoundException(
                            "Workout day not found: " + req.workoutDayId()));
            session.setWorkoutDayId(req.workoutDayId());
        }
        return SessionsMapper.toDto(sessions.save(session));
    }

    @Transactional(readOnly = true)
    public Optional<SessionDto> getActive(UUID userId) {
        return sessions.findByUserIdAndEndedAtIsNull(userId).map(SessionsMapper::toDto);
    }

    public SessionDto finish(UUID userId, UUID sessionId, FinishSessionRequest req) {
        WorkoutSession session = sessions.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new NotFoundException("Session not found: " + sessionId));
        if (session.isFinished()) {
            throw new ConflictException("Session already finished");
        }
        session.setEndedAt(Instant.now());
        if (req != null) {
            if (req.notes() != null) session.setNotes(req.notes());
            if (req.mood() != null) session.setMood(req.mood());
            if (req.energyLevel() != null) session.setEnergyLevel(req.energyLevel());
        }
        return SessionsMapper.toDto(session);
    }

    WorkoutSession findActiveOwnedOrThrow(UUID userId, UUID sessionId) {
        WorkoutSession session = sessions.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new NotFoundException("Session not found: " + sessionId));
        if (session.isFinished()) {
            throw new ConflictException("Session is finished and cannot be modified");
        }
        return session;
    }
}
