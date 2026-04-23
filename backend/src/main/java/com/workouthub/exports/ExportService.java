package com.workouthub.exports;

import com.workouthub.common.web.NotFoundException;
import com.workouthub.exports.dto.ClaudeSummaryDto;
import com.workouthub.exports.dto.ClaudeSummaryDto.ExerciseEntry;
import com.workouthub.exports.dto.ClaudeSummaryDto.Period;
import com.workouthub.exports.dto.ClaudeSummaryDto.SetEntry;
import com.workouthub.exports.dto.ClaudeSummaryDto.Totals;
import com.workouthub.exports.dto.ClaudeSummaryDto.UserSummary;
import com.workouthub.exports.dto.ClaudeSummaryDto.WorkoutEntry;
import com.workouthub.sessions.domain.SessionSet;
import com.workouthub.sessions.domain.WorkoutSession;
import com.workouthub.sessions.domain.WorkoutSessionRepository;
import com.workouthub.users.domain.User;
import com.workouthub.users.domain.UserProfile;
import com.workouthub.users.domain.UserProfileRepository;
import com.workouthub.users.domain.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ExportService {

    private final UserRepository users;
    private final UserProfileRepository profiles;
    private final WorkoutSessionRepository sessions;

    public ExportService(
            UserRepository users,
            UserProfileRepository profiles,
            WorkoutSessionRepository sessions) {
        this.users = users;
        this.profiles = profiles;
        this.sessions = sessions;
    }

    public ClaudeSummaryDto buildSummary(UUID userId, int days) {
        int windowDays = Math.max(1, Math.min(days, 365));
        User user = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        UserProfile profile = profiles.findById(userId).orElse(null);

        Instant since = Instant.now().minus(windowDays, ChronoUnit.DAYS);
        List<WorkoutSession> recent = sessions.findFinishedSince(userId, since);

        List<WorkoutEntry> entries = recent.stream().map(ExportService::toEntry).toList();
        Totals totals = computeTotals(recent);

        return new ClaudeSummaryDto(
                new UserSummary(
                        user.getDisplayName(),
                        user.getEmail(),
                        profile == null ? null : profile.getHeightCm(),
                        profile == null ? null : profile.getWeightKg(),
                        profile == null ? null : profile.getHealthNotes(),
                        profile == null ? null : profile.getGoals()),
                new Period(
                        since.atZone(ZoneOffset.UTC).toLocalDate(),
                        LocalDate.now(ZoneOffset.UTC),
                        windowDays),
                totals,
                entries);
    }

    private static WorkoutEntry toEntry(WorkoutSession session) {
        LocalDate date = session.getStartedAt().atZone(ZoneOffset.UTC).toLocalDate();
        Long durationMin = session.getEndedAt() == null
                ? null
                : Duration.between(session.getStartedAt(), session.getEndedAt()).toMinutes();

        // Preserve insertion order of exercises as encountered in the set list.
        Map<UUID, ExerciseGroup> grouped = new LinkedHashMap<>();
        for (SessionSet set : session.getSets()) {
            if (set.getExercise() == null) continue;
            UUID exId = set.getExercise().getId();
            grouped.computeIfAbsent(exId, k -> new ExerciseGroup(
                    set.getExercise().getNameTr(),
                    set.getExercise().getNameEn(),
                    new ArrayList<>()
            )).sets().add(new SetEntry(set.getRepsDone(), set.getWeightKg()));
        }
        List<ExerciseEntry> exercises = grouped.values().stream()
                .map(g -> new ExerciseEntry(g.nameTr(), g.nameEn(), g.sets()))
                .toList();

        return new WorkoutEntry(
                date,
                durationMin,
                exercises,
                session.getNotes(),
                session.getMood(),
                session.getEnergyLevel());
    }

    private static Totals computeTotals(List<WorkoutSession> sessions) {
        BigDecimal totalVolume = BigDecimal.ZERO;
        long totalDurationMin = 0;
        int durationSamples = 0;
        for (WorkoutSession session : sessions) {
            for (SessionSet set : session.getSets()) {
                BigDecimal w = set.getWeightKg() == null ? BigDecimal.ZERO : set.getWeightKg();
                totalVolume = totalVolume.add(w.multiply(BigDecimal.valueOf(set.getRepsDone())));
            }
            if (session.getEndedAt() != null) {
                totalDurationMin += Duration.between(
                        session.getStartedAt(), session.getEndedAt()).toMinutes();
                durationSamples++;
            }
        }
        Long avg = durationSamples == 0 ? null : totalDurationMin / durationSamples;
        return new Totals(
                sessions.size(),
                totalVolume.setScale(2, RoundingMode.HALF_UP),
                avg);
    }

    private record ExerciseGroup(String nameTr, String nameEn, List<SetEntry> sets) {}
}
