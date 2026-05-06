package com.workouthub.metrics;

import com.workouthub.common.web.NotFoundException;
import com.workouthub.metrics.domain.BodyMetric;
import com.workouthub.metrics.domain.BodyMetricRepository;
import com.workouthub.metrics.dto.BodyMetricDto;
import com.workouthub.metrics.dto.UpsertBodyMetricRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class MetricsService {

    private final BodyMetricRepository repo;

    public MetricsService(BodyMetricRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<BodyMetricDto> list(UUID userId, LocalDate from, LocalDate to) {
        boolean fromSet = from != null;
        boolean toSet = to != null;
        if (fromSet ^ toSet) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "from and to must be provided together");
        }
        if (fromSet && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "from must not be after to");
        }
        List<BodyMetric> rows = fromSet
                ? repo.findByUserIdAndRecordedDateBetweenOrderByRecordedDateDesc(userId, from, to)
                : repo.findByUserIdOrderByRecordedDateDesc(userId);
        return rows.stream().map(MetricsService::toDto).toList();
    }

    public UpsertResult upsert(UUID userId, UpsertBodyMetricRequest req) {
        Optional<BodyMetric> existing = repo.findByUserIdAndRecordedDate(userId, req.recordedDate());
        boolean wasCreated = existing.isEmpty();
        BodyMetric entity = existing.orElseGet(() -> {
            BodyMetric fresh = new BodyMetric();
            fresh.setUserId(userId);
            fresh.setRecordedDate(req.recordedDate());
            return fresh;
        });
        entity.setWeightKg(req.weightKg());
        entity.setBodyFatPercent(req.bodyFatPercent());
        entity.setWaistCm(req.waistCm());
        entity.setChestCm(req.chestCm());
        entity.setArmCm(req.armCm());
        entity.setThighCm(req.thighCm());
        entity.setPhotoUrl(req.photoUrl());
        entity.setNotes(req.notes());
        return new UpsertResult(toDto(repo.save(entity)), wasCreated);
    }

    public void delete(UUID userId, UUID id) {
        BodyMetric entity = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Body metric not found: " + id));
        repo.delete(entity);
    }

    private static BodyMetricDto toDto(BodyMetric m) {
        return new BodyMetricDto(
                m.getId(),
                m.getRecordedDate(),
                m.getWeightKg(),
                m.getBodyFatPercent(),
                m.getWaistCm(),
                m.getChestCm(),
                m.getArmCm(),
                m.getThighCm(),
                m.getPhotoUrl(),
                m.getNotes(),
                m.getCreatedAt(),
                m.getUpdatedAt());
    }

    public static record UpsertResult(BodyMetricDto dto, boolean wasCreated) {}
}
