package com.workouthub.sessions.resttimer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(
        name = "rest_timer_schedules",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "rest_timer_schedules_user_session_unique",
                        columnNames = {"user_id", "session_id"}))
public class RestTimerSchedule {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "fire_at", nullable = false)
    private Instant fireAt;

    @Column(name = "localized_title", nullable = false, columnDefinition = "TEXT")
    private String localizedTitle;

    @Column(name = "localized_body", nullable = false, columnDefinition = "TEXT")
    private String localizedBody;

    @Column(name = "click_url", nullable = false, columnDefinition = "TEXT")
    private String clickUrl;

    @Column(name = "dispatched_at")
    private Instant dispatchedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }

    public Instant getFireAt() { return fireAt; }
    public void setFireAt(Instant fireAt) { this.fireAt = fireAt; }

    public String getLocalizedTitle() { return localizedTitle; }
    public void setLocalizedTitle(String localizedTitle) { this.localizedTitle = localizedTitle; }

    public String getLocalizedBody() { return localizedBody; }
    public void setLocalizedBody(String localizedBody) { this.localizedBody = localizedBody; }

    public String getClickUrl() { return clickUrl; }
    public void setClickUrl(String clickUrl) { this.clickUrl = clickUrl; }

    public Instant getDispatchedAt() { return dispatchedAt; }
    public void setDispatchedAt(Instant dispatchedAt) { this.dispatchedAt = dispatchedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
