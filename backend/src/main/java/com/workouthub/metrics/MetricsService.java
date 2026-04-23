package com.workouthub.metrics;

import com.workouthub.common.web.NotFoundException;
import com.workouthub.metrics.domain.BodyMetric;
import com.workouthub.metrics.domain.BodyMetricRepository;
import com.workouthub.metrics.dto.BodyMetricDto;
import com.workouthub.metrics.dto.UpsertBodyMetricRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MetricsService {

    private final BodyMetricRepository repo;

    public MetricsService(BodyMetricRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<BodyMetricDto> list(UUID userId) {
        return repo.findByUserIdOrderByRecordedDateDesc(userId).stream()
                .map(MetricsService::toDto)
                .toList();
    }

    public BodyMetricDto upsert(UUID userId, UpsertBodyMetricRequest req) {
        BodyMetric entity = repo
                .findByUserIdAndRecordedDate(userId, req.recordedDate())
                .orElseGet(() -> {
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
        entity.setNotes(req.notes());
        return toDto(repo.save(entity));
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
}
