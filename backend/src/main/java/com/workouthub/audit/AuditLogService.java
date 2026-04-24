package com.workouthub.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workouthub.audit.domain.AuditLog;
import com.workouthub.audit.domain.AuditLogRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuditLogService {

    private final AuditLogRepository repo;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository repo, ObjectMapper objectMapper) {
        this.repo = repo;
        this.objectMapper = objectMapper;
    }

    public void record(
            UUID actorId, String action, String targetType, UUID targetId, Object payload) {
        AuditLog row = new AuditLog();
        row.setActorId(actorId);
        row.setAction(action);
        row.setTargetType(targetType);
        row.setTargetId(targetId);
        row.setPayloadJson(toJson(payload));
        repo.save(row);
    }

    private String toJson(Object payload) {
        if (payload == null) return null;
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return "{\"error\":\"serialization-failed\"}";
        }
    }
}
