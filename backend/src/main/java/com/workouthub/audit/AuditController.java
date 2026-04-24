package com.workouthub.audit;

import com.workouthub.audit.domain.AuditLog;
import com.workouthub.audit.domain.AuditLogRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit")
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    public record AuditEntryDto(
            UUID id,
            UUID actorId,
            String action,
            String targetType,
            UUID targetId,
            String payloadJson,
            Instant at) {}

    private final AuditLogRepository repo;

    public AuditController(AuditLogRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<AuditEntryDto> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return repo.findAllByOrderByAtDesc(PageRequest.of(page, Math.min(size, 200)))
                .stream()
                .map(AuditController::toDto)
                .toList();
    }

    private static AuditEntryDto toDto(AuditLog row) {
        return new AuditEntryDto(
                row.getId(),
                row.getActorId(),
                row.getAction(),
                row.getTargetType(),
                row.getTargetId(),
                row.getPayloadJson(),
                row.getAt());
    }
}
