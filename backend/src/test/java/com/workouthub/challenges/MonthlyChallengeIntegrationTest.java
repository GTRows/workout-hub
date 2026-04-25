package com.workouthub.challenges;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.workouthub.challenges.domain.MonthlyChallenge;
import com.workouthub.challenges.domain.MonthlyChallengeRepository;
import com.workouthub.sessions.domain.WorkoutSession;
import com.workouthub.sessions.domain.WorkoutSessionRepository;
import com.workouthub.support.AbstractIntegrationTest;
import com.workouthub.support.TestAuthHelpers;
import com.workouthub.support.TestAuthHelpers.SeededUser;
import com.workouthub.users.domain.Role;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@Transactional
class MonthlyChallengeIntegrationTest extends AbstractIntegrationTest {

    private static final String SECRET = "ChalSecret1!";

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;
    @Autowired MonthlyChallengeRepository repo;
    @Autowired WorkoutSessionRepository sessions;

    @Test
    void adminCanCreateAChallenge() throws Exception {
        SeededUser admin = helpers.seed(
                "chadmin-" + System.nanoTime() + "@test.local", SECRET, Role.ADMIN);
        String ym = YearMonth.now().toString();
        String body = """
                {"yearMonth":"%s","nameTr":"Bu ay","nameEn":"This month",
                 "ruleType":"session_count","threshold":12}
                """.formatted(ym);

        mvc.perform(post("/api/admin/challenges")
                        .header("Authorization", "Bearer " + admin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.yearMonth").value(ym));
    }

    @Test
    void currentReturnsLiveProgressForUserInThisMonth() throws Exception {
        SeededUser u = helpers.seed(
                "chuser-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        MonthlyChallenge ch = new MonthlyChallenge();
        ch.setYearMonth(YearMonth.now().toString());
        ch.setNameTr("Test"); ch.setNameEn("Test");
        ch.setRuleType("session_count");
        ch.setThreshold(5);
        repo.save(ch);

        seedFinishedSession(u, LocalDate.now().withDayOfMonth(1));
        seedFinishedSession(u, LocalDate.now().withDayOfMonth(2));

        mvc.perform(get("/api/challenges/current")
                        .header("Authorization", "Bearer " + u.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentValue").value(2))
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.challenge.threshold").value(5));
    }

    @Test
    void countsOnlyCurrentMonthSessions() throws Exception {
        SeededUser u = helpers.seed(
                "chuser2-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        MonthlyChallenge ch = new MonthlyChallenge();
        ch.setYearMonth(YearMonth.now().toString());
        ch.setNameTr("Test"); ch.setNameEn("Test");
        ch.setRuleType("session_count");
        ch.setThreshold(5);
        repo.save(ch);

        // One in this month, one in the previous month -- previous should be ignored.
        seedFinishedSession(u, LocalDate.now().withDayOfMonth(1));
        seedFinishedSession(u, LocalDate.now().minusMonths(1).withDayOfMonth(15));

        mvc.perform(get("/api/challenges/current")
                        .header("Authorization", "Bearer " + u.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentValue").value(1));
    }

    @Test
    void returnsNoContentWhenThereIsNoChallengeForThisMonth() throws Exception {
        SeededUser u = helpers.seed(
                "chuser3-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        mvc.perform(get("/api/challenges/current")
                        .header("Authorization", "Bearer " + u.accessToken()))
                .andExpect(status().isNoContent());
    }

    private void seedFinishedSession(SeededUser u, LocalDate localDate) {
        WorkoutSession s = new WorkoutSession();
        s.setUserId(u.id());
        s.setStartedAt(localDate.atTime(10, 0).atZone(ZoneId.systemDefault()).toInstant());
        s.setEndedAt(localDate.atTime(11, 0).atZone(ZoneId.systemDefault()).toInstant());
        sessions.save(s);
    }
}
