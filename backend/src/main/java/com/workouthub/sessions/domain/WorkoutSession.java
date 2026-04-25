package com.workouthub.sessions.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "workout_sessions")
public class WorkoutSession {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "workout_day_id")
    private UUID workoutDayId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column
    private Short mood;

    @Column(name = "energy_level")
    private Short energyLevel;

    @Column(name = "heart_rate_avg_bpm")
    private Short heartRateAvgBpm;

    @OneToMany(
            mappedBy = "session",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<SessionSet> sets = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (this.startedAt == null) this.startedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addSet(SessionSet set) {
        set.setSession(this);
        sets.add(set);
    }

    public void removeSet(SessionSet set) {
        sets.remove(set);
        set.setSession(null);
    }

    public boolean isFinished() {
        return endedAt != null;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getWorkoutDayId() { return workoutDayId; }
    public void setWorkoutDayId(UUID workoutDayId) { this.workoutDayId = workoutDayId; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Short getMood() { return mood; }
    public void setMood(Short mood) { this.mood = mood; }

    public Short getEnergyLevel() { return energyLevel; }
    public void setEnergyLevel(Short energyLevel) { this.energyLevel = energyLevel; }

    public Short getHeartRateAvgBpm() { return heartRateAvgBpm; }
    public void setHeartRateAvgBpm(Short heartRateAvgBpm) { this.heartRateAvgBpm = heartRateAvgBpm; }

    public List<SessionSet> getSets() { return sets; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
