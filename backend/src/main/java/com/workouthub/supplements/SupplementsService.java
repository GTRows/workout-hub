package com.workouthub.supplements;

import com.workouthub.common.web.NotFoundException;
import com.workouthub.supplements.domain.Supplement;
import com.workouthub.supplements.domain.SupplementRepository;
import com.workouthub.supplements.dto.CreateSupplementRequest;
import com.workouthub.supplements.dto.SupplementDto;
import com.workouthub.supplements.dto.UpdateSupplementRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SupplementsService {

    private final SupplementRepository repo;

    public SupplementsService(SupplementRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<SupplementDto> list(UUID userId) {
        return repo.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(SupplementsService::toDto)
                .toList();
    }

    public SupplementDto create(UUID userId, CreateSupplementRequest req) {
        Supplement s = new Supplement();
        s.setUserId(userId);
        s.setName(req.name().trim());
        s.setDosage(req.dosage());
        s.setTiming(req.timing());
        s.setActive(true);
        s.setReminderTime(req.reminderTime());
        return toDto(repo.save(s));
    }

    public SupplementDto update(UUID userId, UUID id, UpdateSupplementRequest req) {
        Supplement s = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Supplement not found: " + id));
        if (req.name() != null && !req.name().isBlank()) s.setName(req.name().trim());
        if (req.dosage() != null) s.setDosage(req.dosage());
        if (req.timing() != null) s.setTiming(req.timing());
        if (req.active() != null) s.setActive(req.active());
        if (req.reminderTime() != null) s.setReminderTime(req.reminderTime());
        return toDto(s);
    }

    public void delete(UUID userId, UUID id) {
        Supplement s = repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Supplement not found: " + id));
        repo.delete(s);
    }

    private static SupplementDto toDto(Supplement s) {
        return new SupplementDto(
                s.getId(),
                s.getName(),
                s.getDosage(),
                s.getTiming(),
                s.isActive(),
                s.getReminderTime(),
                s.getCreatedAt(),
                s.getUpdatedAt());
    }
}
