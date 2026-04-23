package com.workouthub.workouts;

import com.workouthub.exercises.domain.Exercise;
import com.workouthub.exercises.domain.ExerciseRepository;
import com.workouthub.workouts.domain.WorkoutDay;
import com.workouthub.workouts.domain.WorkoutDayExercise;
import com.workouthub.workouts.domain.WorkoutFocus;
import com.workouthub.workouts.domain.WorkoutPlan;
import com.workouthub.workouts.domain.WorkoutPlanRepository;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Materializes the built-in "Baslangic Plani" starter plan for a newly
 * created user. Called by AdminUsersService after a successful create.
 *
 * The template references seed exercises by name_en (inserted in V9). If a
 * referenced exercise is missing (e.g., a minimal test environment without
 * the seed migration), the seeder logs and skips that item rather than
 * failing the surrounding user-create flow.
 */
@Component
public class DefaultPlanSeeder {

    private static final Logger log = LoggerFactory.getLogger(DefaultPlanSeeder.class);

    private static final List<DaySpec> TEMPLATE = List.of(
            new DaySpec((short) 1, "Upper Body Push", WorkoutFocus.PUSH, List.of(
                    new ItemSpec("Push-up", 3, 8, 12),
                    new ItemSpec("Dumbbell Shoulder Press", 3, 8, 10),
                    new ItemSpec("Lateral Raise", 3, 12, 15))),
            new DaySpec((short) 3, "Lower Body", WorkoutFocus.LEGS, List.of(
                    new ItemSpec("Bodyweight Squat", 3, 12, 15),
                    new ItemSpec("Lunge", 3, 10, 12),
                    new ItemSpec("Glute Bridge", 3, 12, 15))),
            new DaySpec((short) 5, "Upper Body Pull + Core", WorkoutFocus.PULL, List.of(
                    new ItemSpec("Negative Pull-up", 3, 3, 5),
                    new ItemSpec("Single-arm Row", 3, 10, 12),
                    new ItemSpec("Plank", 3, 30, 45),
                    new ItemSpec("Dead Bug", 3, 8, 10))));

    private final WorkoutPlanRepository plans;
    private final ExerciseRepository exercises;

    public DefaultPlanSeeder(WorkoutPlanRepository plans, ExerciseRepository exercises) {
        this.plans = plans;
        this.exercises = exercises;
    }

    @Transactional
    public void seedFor(UUID userId) {
        if (plans.findByUserIdAndActiveTrue(userId).isPresent()) {
            return;
        }

        WorkoutPlan plan = new WorkoutPlan();
        plan.setUserId(userId);
        plan.setName("Baslangic Plani");
        plan.setActive(true);

        for (DaySpec dayTemplate : TEMPLATE) {
            WorkoutDay day = new WorkoutDay();
            day.setDayOfWeek(dayTemplate.dayOfWeek());
            day.setName(dayTemplate.name());
            day.setFocus(dayTemplate.focus());

            int order = 1;
            for (ItemSpec itemSpec : dayTemplate.items()) {
                Exercise exercise = exercises.findByNameEnIgnoreCase(itemSpec.nameEn()).orElse(null);
                if (exercise == null) {
                    log.warn("Default plan seed: exercise '{}' missing, skipping", itemSpec.nameEn());
                    continue;
                }
                WorkoutDayExercise item = new WorkoutDayExercise();
                item.setExercise(exercise);
                item.setOrderIndex(order++);
                item.setTargetSets(itemSpec.sets());
                item.setTargetRepsMin(itemSpec.repsMin());
                item.setTargetRepsMax(itemSpec.repsMax());
                day.addExercise(item);
            }
            plan.addDay(day);
        }

        plans.save(plan);
        log.info("Seeded default plan for user {}", userId);
    }

    private record DaySpec(short dayOfWeek, String name, WorkoutFocus focus, List<ItemSpec> items) {}

    private record ItemSpec(String nameEn, int sets, int repsMin, int repsMax) {}
}
