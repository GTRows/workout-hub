package com.workouthub.health;

import com.workouthub.health.dto.HealthImportResultDto;
import com.workouthub.metrics.domain.BodyMetric;
import com.workouthub.metrics.domain.BodyMetricRepository;
import com.workouthub.sessions.domain.WorkoutSession;
import com.workouthub.sessions.domain.WorkoutSessionRepository;
import java.io.InputStream;
import java.util.UUID;
import javax.xml.stream.XMLStreamException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HealthImportService {

    private final BodyMetricRepository bodyMetricRepo;
    private final WorkoutSessionRepository sessionRepo;

    public HealthImportService(
            BodyMetricRepository bodyMetricRepo,
            WorkoutSessionRepository sessionRepo) {
        this.bodyMetricRepo = bodyMetricRepo;
        this.sessionRepo = sessionRepo;
    }

    @Transactional
    public HealthImportResultDto importApple(
            UUID userId,
            InputStream stream,
            boolean importBodyMass,
            boolean importWorkouts) throws Exception {

        ParsedHealth parsed;
        try {
            parsed = new AppleHealthParser().parse(stream);
        } catch (XMLStreamException e) {
            throw new IllegalArgumentException("Invalid Apple Health XML: " + e.getMessage(), e);
        }

        return apply(userId, parsed, importBodyMass, importWorkouts);
    }

    @Transactional
    public HealthImportResultDto importGoogleFit(
            UUID userId,
            InputStream stream,
            boolean importBodyMass,
            boolean importWorkouts) throws Exception {
        ParsedHealth parsed = new GoogleFitParser().parse(stream);
        return apply(userId, parsed, importBodyMass, importWorkouts);
    }

    @Transactional
    public HealthImportResultDto importGarminFit(UUID userId, InputStream stream) throws Exception {
        GarminFitParser.Session s = new GarminFitParser().parse(stream);

        boolean dup = sessionRepo.findFinishedSince(userId, s.startedAt()).stream()
                .anyMatch(existing -> existing.getStartedAt().equals(s.startedAt()));
        if (dup) {
            return new HealthImportResultDto(0, 0, 0, 1, 0, 1);
        }
        WorkoutSession session = new WorkoutSession();
        session.setUserId(userId);
        session.setStartedAt(s.startedAt());
        session.setEndedAt(s.endedAt());
        session.setNotes("Imported from Garmin .fit");
        session.setHeartRateAvgBpm(s.avgHeartRateBpm());
        sessionRepo.save(session);
        return new HealthImportResultDto(0, 0, 0, 1, 1, 0);
    }

    private HealthImportResultDto apply(
            UUID userId,
            ParsedHealth parsed,
            boolean importBodyMass,
            boolean importWorkouts) {
        int bmImported = 0, bmSkipped = 0;
        if (importBodyMass) {
            for (ParsedHealth.BodyMassRecord rec : parsed.bodyMass()) {
                if (bodyMetricRepo.findByUserIdAndRecordedDate(userId, rec.date()).isPresent()) {
                    bmSkipped++;
                    continue;
                }
                BodyMetric m = new BodyMetric();
                m.setUserId(userId);
                m.setRecordedDate(rec.date());
                m.setWeightKg(rec.kg());
                bodyMetricRepo.save(m);
                bmImported++;
            }
        }

        int wImported = 0, wSkipped = 0;
        if (importWorkouts) {
            for (ParsedHealth.WorkoutRecord rec : parsed.workouts()) {
                boolean dup = sessionRepo.findFinishedSince(userId, rec.startedAt()).stream()
                        .anyMatch(s -> s.getStartedAt().equals(rec.startedAt()));
                if (dup) {
                    wSkipped++;
                    continue;
                }
                WorkoutSession s = new WorkoutSession();
                s.setUserId(userId);
                s.setStartedAt(rec.startedAt());
                s.setEndedAt(rec.endedAt());
                s.setNotes(rec.notes());
                sessionRepo.save(s);
                wImported++;
            }
        }

        return new HealthImportResultDto(
                parsed.bodyMass().size(), bmImported, bmSkipped,
                parsed.workouts().size(), wImported, wSkipped);
    }
}
