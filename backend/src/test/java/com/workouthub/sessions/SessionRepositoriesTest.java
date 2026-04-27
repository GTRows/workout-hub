package com.workouthub.sessions;

import static org.assertj.core.api.Assertions.assertThat;

import com.workouthub.exercises.domain.Category;
import com.workouthub.exercises.domain.Difficulty;
import com.workouthub.exercises.domain.Equipment;
import com.workouthub.exercises.domain.Exercise;
import com.workouthub.exercises.domain.ExerciseRepository;
import com.workouthub.sessions.domain.SessionSet;
import com.workouthub.sessions.domain.SessionSetRepository;
import com.workouthub.sessions.domain.WorkoutSession;
import com.workouthub.sessions.domain.WorkoutSessionRepository;
import com.workouthub.support.AbstractIntegrationTest;
import com.workouthub.support.TestAuthHelpers;
import com.workouthub.support.TestAuthHelpers.SeededUser;
import com.workouthub.users.domain.Role;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class SessionRepositoriesTest extends AbstractIntegrationTest {

    @Autowired WorkoutSessionRepository sessions;
    @Autowired SessionSetRepository sets;
    @Autowired ExerciseRepository exercises;
    @Autowired TestAuthHelpers helpers;

    @Test
    void roundTripSessionWithNestedSets() {
        SeededUser user = helpers.seed(
                "sess-" + System.nanoTime() + "@test.local", "Secret1!", Role.USER);
        Exercise ex = seedExercise("RT-" + System.nanoTime());

        WorkoutSession session = new WorkoutSession();
        session.setUserId(user.id());

        SessionSet s1 = newSet(ex, (short) 1, (short) 10, new BigDecimal("60.00"));
        SessionSet s2 = newSet(ex, (short) 2, (short) 10, new BigDecimal("60.00"));
        session.addSet(s1);
        session.addSet(s2);

        WorkoutSession saved = sessions.saveAndFlush(session);
        sessions.findById(saved.getId()).ifPresent(s -> {
            assertThat(s.isFinished()).isFalse();
            assertThat(s.getSets()).hasSize(2);
        });
    }

    @Test
    void findByUserIdAndEndedAtIsNullReturnsActiveOnly() {
        SeededUser user = helpers.seed(
                "act-" + System.nanoTime() + "@test.local", "Secret1!", Role.USER);

        WorkoutSession finished = new WorkoutSession();
        finished.setUserId(user.id());
        finished.setStartedAt(Instant.now().minusSeconds(3600));
        finished.setEndedAt(Instant.now());
        sessions.save(finished);

        WorkoutSession active = new WorkoutSession();
        active.setUserId(user.id());
        sessions.save(active);

        var found = sessions.findByUserIdAndEndedAtIsNull(user.id());
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(active.getId());
    }

    @Test
    void cascadeDeleteSessionRemovesItsSets() {
        SeededUser user = helpers.seed(
                "cas-" + System.nanoTime() + "@test.local", "Secret1!", Role.USER);
        Exercise ex = seedExercise("Cas-" + System.nanoTime());

        WorkoutSession session = new WorkoutSession();
        session.setUserId(user.id());
        session.addSet(newSet(ex, (short) 1, (short) 8, null));
        session.addSet(newSet(ex, (short) 2, (short) 8, null));
        sessions.saveAndFlush(session);
        UUID sessionId = session.getId();

        sessions.delete(session);
        sessions.flush();

        assertThat(sessions.findById(sessionId)).isEmpty();
        assertThat(sets.countBySessionIdAndExerciseId(sessionId, ex.getId())).isZero();
    }

    @Test
    void findBySessionAndExerciseReturnsSortedSets() {
        SeededUser user = helpers.seed(
                "fbe-" + System.nanoTime() + "@test.local", "Secret1!", Role.USER);
        Exercise ex = seedExercise("FbE-" + System.nanoTime());

        WorkoutSession session = new WorkoutSession();
        session.setUserId(user.id());
        session.addSet(newSet(ex, (short) 3, (short) 5, null));
        session.addSet(newSet(ex, (short) 1, (short) 10, null));
        session.addSet(newSet(ex, (short) 2, (short) 8, null));
        sessions.saveAndFlush(session);

        var list = sets.findBySessionIdAndExerciseIdOrderBySetNumberAsc(
                session.getId(), ex.getId());
        assertThat(list).extracting(SessionSet::getSetNumber)
                .containsExactly((short) 1, (short) 2, (short) 3);
    }

    private Exercise seedExercise(String tag) {
        Exercise e = new Exercise();
        e.setNameTr("TR " + tag);
        e.setNameEn("EN " + tag);
        e.setCategory(Category.PUSH);
        e.setEquipment(Equipment.DUMBBELL);
        e.setMusclePrimary("chest");
        e.setDifficulty(Difficulty.BEGINNER);
        return exercises.save(e);
    }

    private static SessionSet newSet(Exercise ex, short setNumber, short reps, BigDecimal weight) {
        SessionSet s = new SessionSet();
        s.setExercise(ex);
        s.setSetNumber(setNumber);
        s.setRepsDone(reps);
        s.setWeightKg(weight);
        s.setCompleted(true);
        return s;
    }
}
