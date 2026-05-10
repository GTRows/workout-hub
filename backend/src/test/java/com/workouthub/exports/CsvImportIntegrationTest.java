package com.workouthub.exports;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.workouthub.exercises.domain.Category;
import com.workouthub.exercises.domain.Difficulty;
import com.workouthub.exercises.domain.Equipment;
import com.workouthub.exercises.domain.Exercise;
import com.workouthub.exercises.domain.ExerciseRepository;
import com.workouthub.support.AbstractIntegrationTest;
import com.workouthub.support.TestAuthHelpers;
import com.workouthub.support.TestAuthHelpers.SeededUser;
import com.workouthub.users.domain.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class CsvImportIntegrationTest extends AbstractIntegrationTest {

    private static final String SECRET = "CsvImpSecret1!";

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;
    @Autowired ExerciseRepository exerciseRepo;

    @Test
    void importsTwoSetsAndReportsOneUnmatchedName() throws Exception {
        SeededUser u = helpers.seed(
                "csv-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + u.accessToken();
        seedExercise("Bench Press");

        String csv = """
                Date,Workout Name,Exercise Name,Set Order,Weight,Reps,Notes,Workout Notes
                2026-04-20 09:00:00,Morning,Bench Press,1,80,8,,felt strong
                2026-04-20 09:00:00,Morning,Bench Press,2,80,8,,felt strong
                2026-04-20 09:00:00,Morning,Zercher Carry,1,60,10,,felt strong
                """;

        mvc.perform(post("/api/export/import/csv")
                        .header("Authorization", auth)
                        .contentType(MediaType.parseMediaType("text/csv"))
                        .content(csv))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.setsInserted").value(2))
                .andExpect(jsonPath("$.sessionsInserted").value(1))
                .andExpect(jsonPath("$.unmatchedExerciseNames.length()").value(1))
                .andExpect(jsonPath("$.unmatchedExerciseNames[0]").value("Zercher Carry"));
    }

    @Test
    void emptyBodyReturnsZeroesNotAnError() throws Exception {
        SeededUser u = helpers.seed(
                "csv2-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        mvc.perform(post("/api/export/import/csv")
                        .header("Authorization", "Bearer " + u.accessToken())
                        .contentType(MediaType.parseMediaType("text/csv"))
                        .content(""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.setsInserted").value(0));
    }

    private void seedExercise(String name) {
        Exercise e = new Exercise();
        e.setNameTr(name);
        e.setNameEn(name);
        e.setCategory(Category.PUSH);
        e.setEquipment(Equipment.BAR);
        e.setMusclePrimary("chest");
        e.setDifficulty(Difficulty.INTERMEDIATE);
        exerciseRepo.save(e);
    }
}
