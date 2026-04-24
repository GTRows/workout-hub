package com.workouthub.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(name = "target_type", nullable = false, length = 60)
    private String targetType;

    @Column(name = "target_id")
    private UUID targetId;

    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;

    @Column(nullable = false)
    private Instant at;

    @PrePersist
    void onCreate() {
        if (this.at == null) this.at = Instant.now();
    }

    public UUID getId() { return id; }

    public UUID getActorId() { return actorId; }
    public void setActorId(UUID actorId) { this.actorId = actorId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public UUID getTargetId() { return targetId; }
    public void setTargetId(UUID targetId) { this.targetId = targetId; }

    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }

    public Instant getAt() { return at; }
    public void setAt(Instant at) { this.at = at; }
}
