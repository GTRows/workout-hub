package com.workouthub.exports;

import com.workouthub.common.web.NotFoundException;
import com.workouthub.exports.dto.FullExportDto;
import com.workouthub.exports.dto.ImportResultDto;
import com.workouthub.metrics.domain.BodyMetric;
import com.workouthub.metrics.domain.BodyMetricRepository;
import com.workouthub.supplements.domain.Supplement;
import com.workouthub.supplements.domain.SupplementRepository;
import com.workouthub.supplements.domain.SupplementTiming;
import com.workouthub.users.domain.User;
import com.workouthub.users.domain.UserProfile;
import com.workouthub.users.domain.UserProfileRepository;
import com.workouthub.users.domain.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/**
 * Idempotent import of the profile, supplements, and body_metrics slices
 * of the full-export dump. Plans and sessions are currently rejected
 * (HTTP 422) until the plan/session import pipeline lands; see the t-56
 * follow-up in TODO.md.
 */
@Service
@Transactional
public class FullImportService {

    private static final int SUPPORTED_SCHEMA_VERSION = 1;

    private final UserRepository users;
    private final UserProfileRepository profiles;
    private final BodyMetricRepository metrics;
    private final SupplementRepository supplements;

    public FullImportService(
            UserRepository users,
            UserProfileRepository profiles,
            BodyMetricRepository metrics,
            SupplementRepository supplements) {
        this.users = users;
        this.profiles = profiles;
        this.metrics = metrics;
        this.supplements = supplements;
    }

    public ImportResultDto importDump(UUID userId, FullExportDto dump) {
        if (dump == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty import payload");
        }
        if (dump.schemaVersion() != SUPPORTED_SCHEMA_VERSION) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Unsupported schemaVersion " + dump.schemaVersion()
                            + "; expected " + SUPPORTED_SCHEMA_VERSION);
        }
        if ((dump.plans() != null && !dump.plans().isEmpty())
                || (dump.sessions() != null && !dump.sessions().isEmpty())) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Importing plans and sessions is not supported yet; "
                            + "remove them from the payload (see t-56)");
        }

        User user = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        int profileUpdated = importProfile(userId, dump.user());
        int metricsInserted = replaceMetrics(userId, dump.bodyMetrics());
        int supplementsInserted = replaceSupplements(userId, dump.supplements());

        return new ImportResultDto(
                profileUpdated, metricsInserted, supplementsInserted, user.getEmail());
    }

    private int importProfile(UUID userId, FullExportDto.UserSection section) {
        if (section == null) return 0;
        UserProfile profile = profiles.findById(userId).orElseGet(() -> {
            UserProfile fresh = new UserProfile();
            User owner = users.findById(userId).orElseThrow();
            fresh.setUser(owner);
            return fresh;
        });
        profile.setHeightCm(section.heightCm());
        profile.setWeightKg(section.weightKg());
        profile.setBirthDate(section.birthDate());
        profile.setGender(section.gender());
        profile.setHealthNotes(section.healthNotes());
        profile.setGoals(section.goals());
        profiles.save(profile);
        return 1;
    }

    private int replaceMetrics(UUID userId, List<FullExportDto.MetricRow> rows) {
        if (rows == null) return 0;
        List<BodyMetric> existing = metrics.findByUserIdOrderByRecordedDateDesc(userId);
        metrics.deleteAll(existing);
        metrics.flush();

        List<BodyMetric> fresh = new ArrayList<>(rows.size());
        for (FullExportDto.MetricRow r : rows) {
            BodyMetric m = new BodyMetric();
            m.setUserId(userId);
            m.setRecordedDate(r.recordedDate());
            m.setWeightKg(r.weightKg());
            m.setBodyFatPercent(r.bodyFatPercent());
            m.setWaistCm(r.waistCm());
            m.setChestCm(r.chestCm());
            m.setArmCm(r.armCm());
            m.setThighCm(r.thighCm());
            m.setPhotoUrl(r.photoUrl());
            m.setNotes(r.notes());
            fresh.add(m);
        }
        metrics.saveAll(fresh);
        return fresh.size();
    }

    private int replaceSupplements(UUID userId, List<FullExportDto.SupplementRow> rows) {
        if (rows == null) return 0;
        List<Supplement> existing = supplements.findByUserIdOrderByCreatedAtAsc(userId);
        supplements.deleteAll(existing);
        supplements.flush();

        List<Supplement> fresh = new ArrayList<>(rows.size());
        for (FullExportDto.SupplementRow r : rows) {
            Supplement s = new Supplement();
            s.setUserId(userId);
            s.setName(r.name());
            s.setDosage(r.dosage());
            s.setTiming(parseTiming(r.timing()));
            s.setActive(r.active());
            fresh.add(s);
        }
        supplements.saveAll(fresh);
        return fresh.size();
    }

    private static SupplementTiming parseTiming(String s) {
        if (s == null) return SupplementTiming.OTHER;
        try {
            return SupplementTiming.valueOf(s.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Unknown supplement timing value: " + s);
        }
    }
}
