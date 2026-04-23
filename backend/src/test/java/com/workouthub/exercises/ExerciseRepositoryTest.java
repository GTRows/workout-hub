package com.workouthub.exercises;

import static org.assertj.core.api.Assertions.assertThat;

import com.workouthub.exercises.domain.Category;
import com.workouthub.exercises.domain.Difficulty;
import com.workouthub.exercises.domain.Equipment;
import com.workouthub.exercises.domain.Exercise;
import com.workouthub.exercises.domain.ExerciseRepository;
import com.workouthub.support.AbstractIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ExerciseRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    ExerciseRepository repo;

    @Test
    void roundTripPreservesEnumsAsLowercaseInDbAndUppercaseInJava() {
        long tag = System.nanoTime();
        Exercise pushUp = newExercise("Sinav-" + tag, "Push-up-" + tag,
                Category.PUSH, Equipment.BODYWEIGHT, Difficulty.BEGINNER);
        pushUp.setFormTipsTr(List.of("Govdeyi sik tut", "Dirsekler 45 derece"));
        pushUp.setFormTipsEn(List.of("Keep core tight", "Elbows 45 degrees"));
        pushUp.setCommonMistakesTr(List.of("Dirsek acik"));
        pushUp.setCommonMistakesEn(List.of("Flared elbows"));

        Exercise saved = repo.saveAndFlush(pushUp);
        repo.findById(saved.getId()).ifPresent(e -> {
            assertThat(e.getCategory()).isEqualTo(Category.PUSH);
            assertThat(e.getEquipment()).isEqualTo(Equipment.BODYWEIGHT);
            assertThat(e.getDifficulty()).isEqualTo(Difficulty.BEGINNER);
            assertThat(e.getFormTipsTr()).containsExactly("Govdeyi sik tut", "Dirsekler 45 derece");
            assertThat(e.getFormTipsEn()).containsExactly("Keep core tight", "Elbows 45 degrees");
            assertThat(e.getCommonMistakesTr()).containsExactly("Dirsek acik");
            assertThat(e.getCommonMistakesEn()).containsExactly("Flared elbows");
        });
    }

    @Test
    void findByCategoryFiltersResults() {
        repo.save(newExercise("Bench", "Bench Press " + System.nanoTime(),
                Category.PUSH, Equipment.DUMBBELL, Difficulty.INTERMEDIATE));
        repo.save(newExercise("Row", "Row " + System.nanoTime(),
                Category.PULL, Equipment.DUMBBELL, Difficulty.INTERMEDIATE));

        var pushPage = repo.findByCategory(Category.PUSH, PageRequest.of(0, 20));
        var pullPage = repo.findByCategory(Category.PULL, PageRequest.of(0, 20));

        assertThat(pushPage.getContent()).allMatch(e -> e.getCategory() == Category.PUSH);
        assertThat(pullPage.getContent()).allMatch(e -> e.getCategory() == Category.PULL);
    }

    @Test
    void searchByNameIsCaseInsensitive() {
        String unique = "UniqueMarker" + System.nanoTime();
        repo.save(newExercise("Turk " + unique, "EN " + unique,
                Category.CORE, Equipment.BODYWEIGHT, Difficulty.BEGINNER));

        var byLower = repo.searchByName("uniquemarker", PageRequest.of(0, 20));
        var byUpper = repo.searchByName("UNIQUEMARKER", PageRequest.of(0, 20));

        assertThat(byLower.getTotalElements()).isGreaterThanOrEqualTo(1);
        assertThat(byUpper.getTotalElements()).isEqualTo(byLower.getTotalElements());
    }

    @Test
    void searchCombinesCategoryEquipmentDifficulty() {
        String tag = "Combo" + System.nanoTime();
        repo.save(newExercise("Sade " + tag, "Plain " + tag,
                Category.LEGS, Equipment.DUMBBELL, Difficulty.ADVANCED));

        var all = repo.search(Category.LEGS, Equipment.DUMBBELL, Difficulty.ADVANCED,
                PageRequest.of(0, 20));
        var none = repo.search(Category.LEGS, Equipment.BAR, Difficulty.ADVANCED,
                PageRequest.of(0, 20));

        assertThat(all.getContent()).anyMatch(e -> e.getNameEn().equals("Plain " + tag));
        assertThat(none.getContent()).noneMatch(e -> e.getNameEn().equals("Plain " + tag));
    }

    private static Exercise newExercise(
            String nameTr, String nameEn, Category category, Equipment equipment, Difficulty difficulty) {
        Exercise e = new Exercise();
        e.setNameTr(nameTr);
        e.setNameEn(nameEn);
        e.setCategory(category);
        e.setEquipment(equipment);
        e.setMusclePrimary("chest");
        e.setDifficulty(difficulty);
        return e;
    }
}
