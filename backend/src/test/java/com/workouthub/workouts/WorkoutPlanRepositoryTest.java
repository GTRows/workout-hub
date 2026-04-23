package com.workouthub.workouts;

import static org.assertj.core.api.Assertions.assertThat;

import com.workouthub.exercises.domain.Category;
import com.workouthub.exercises.domain.Difficulty;
import com.workouthub.exercises.domain.Equipment;
import com.workouthub.exercises.domain.Exercise;
import com.workouthub.exercises.domain.ExerciseRepository;
import com.workouthub.support.AbstractIntegrationTest;
import com.workouthub.support.TestAuthHelpers;
import com.workouthub.support.TestAuthHelpers.SeededUser;
import com.workouthub.users.domain.Role;
import com.workouthub.workouts.domain.WorkoutDay;
import com.workouthub.workouts.domain.WorkoutDayExercise;
import com.workouthub.workouts.domain.WorkoutFocus;
import com.workouthub.workouts.domain.WorkoutPlan;
import com.workouthub.workouts.domain.WorkoutPlanRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class WorkoutPlanRepositoryTest extends AbstractIntegrationTest {

    @Autowired WorkoutPlanRepository plans;
    @Autowired ExerciseRepository exercises;
    @Autowired TestAuthHelpers helpers;

    @Test
    void roundTripWithNestedDaysAndExercises() {
        SeededUser user = helpers.seed(
                "plan-" + System.nanoTime() + "@test.local", "Secret1!", Role.USER);
        Exercise ex = seedExercise("Plan-" + System.nanoTime());

        WorkoutPlan plan = new WorkoutPlan();
        plan.setUserId(user.id());
        plan.setName("Test Plan");
        plan.setActive(true);

        WorkoutDay monday = new WorkoutDay();
        monday.setDayOfWeek((short) 1);
        monday.setName("Monday Push");
        monday.setFocus(WorkoutFocus.PUSH);
        monday.setEstimatedDurationMin(45);

        WorkoutDayExercise item = new WorkoutDayExercise();
        item.setExercise(ex);
        item.setOrderIndex(1);
        item.setTargetSets(3);
        item.setTargetRepsMin(8);
        item.setTargetRepsMax(12);

        monday.addExercise(item);
        plan.addDay(monday);

        WorkoutPlan saved = plans.saveAndFlush(plan);

        plans.findById(saved.getId()).ifPresent(p -> {
            assertThat(p.getName()).isEqualTo("Test Plan");
            assertThat(p.isActive()).isTrue();
            assertThat(p.getDays()).hasSize(1);
            WorkoutDay d = p.getDays().get(0);
            assertThat(d.getDayOfWeek()).isEqualTo((short) 1);
            assertThat(d.getFocus()).isEqualTo(WorkoutFocus.PUSH);
            assertThat(d.getExercises()).hasSize(1);
            assertThat(d.getExercises().get(0).getTargetSets()).isEqualTo(3);
        });
    }

    @Test
    void cascadeDeleteRemovesDaysAndDayExercises() {
        SeededUser user = helpers.seed(
                "cas-" + System.nanoTime() + "@test.local", "Secret1!", Role.USER);
        Exercise ex = seedExercise("Cas-" + System.nanoTime());

        WorkoutPlan plan = new WorkoutPlan();
        plan.setUserId(user.id());
        plan.setName("Cascade Plan");

        WorkoutDay day = new WorkoutDay();
        day.setDayOfWeek((short) 2);
        day.setName("Day");
        day.setFocus(WorkoutFocus.LEGS);

        WorkoutDayExercise item = new WorkoutDayExercise();
        item.setExercise(ex);
        item.setOrderIndex(1);
        item.setTargetSets(3);
        day.addExercise(item);
        plan.addDay(day);

        plans.saveAndFlush(plan);
        var planId = plan.getId();

        plans.delete(plan);
        plans.flush();

        assertThat(plans.findById(planId)).isEmpty();
    }

    @Test
    void orphanRemovalDropsDayWhenRemovedFromPlan() {
        SeededUser user = helpers.seed(
                "orp-" + System.nanoTime() + "@test.local", "Secret1!", Role.USER);

        WorkoutPlan plan = new WorkoutPlan();
        plan.setUserId(user.id());
        plan.setName("Orphan Plan");
        WorkoutDay day = new WorkoutDay();
        day.setDayOfWeek((short) 3);
        day.setName("Temp Day");
        day.setFocus(WorkoutFocus.CORE);
        plan.addDay(day);

        plans.saveAndFlush(plan);
        plan.removeDay(day);
        plans.saveAndFlush(plan);

        plans.findById(plan.getId()).ifPresent(p -> assertThat(p.getDays()).isEmpty());
    }

    @Test
    void findByUserIdAndActiveTrueReturnsActivePlan() {
        SeededUser user = helpers.seed(
                "act-" + System.nanoTime() + "@test.local", "Secret1!", Role.USER);

        WorkoutPlan active = new WorkoutPlan();
        active.setUserId(user.id());
        active.setName("Active");
        active.setActive(true);
        plans.save(active);

        WorkoutPlan inactive = new WorkoutPlan();
        inactive.setUserId(user.id());
        inactive.setName("Inactive");
        inactive.setActive(false);
        plans.save(inactive);

        var found = plans.findByUserIdAndActiveTrue(user.id());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Active");
    }

    private Exercise seedExercise(String uniqueTag) {
        Exercise e = new Exercise();
        e.setNameTr("TR " + uniqueTag);
        e.setNameEn("EN " + uniqueTag);
        e.setCategory(Category.PUSH);
        e.setEquipment(Equipment.DUMBBELL);
        e.setMusclePrimary("chest");
        e.setDifficulty(Difficulty.BEGINNER);
        return exercises.save(e);
    }
}
