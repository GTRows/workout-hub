package com.workouthub.achievements.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "user_achievements")
public class UserAchievement {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "achievement_id", nullable = false)
    private UUID achievementId;

    @Column(name = "unlocked_at", nullable = false, updatable = false)
    private Instant unlockedAt;

    @Column(name = "progress_value")
    private Integer progressValue;

    @PrePersist
    void onCreate() {
        if (this.unlockedAt == null) this.unlockedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getAchievementId() { return achievementId; }
    public void setAchievementId(UUID achievementId) { this.achievementId = achievementId; }

    public Instant getUnlockedAt() { return unlockedAt; }

    public Integer getProgressValue() { return progressValue; }
    public void setProgressValue(Integer progressValue) { this.progressValue = progressValue; }
}
