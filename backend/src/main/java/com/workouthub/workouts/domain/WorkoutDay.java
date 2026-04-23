package com.workouthub.workouts.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "workout_days")
public class WorkoutDay {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private WorkoutPlan plan;

    @Column(name = "day_of_week", nullable = false)
    private short dayOfWeek;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 20)
    private WorkoutFocus focus;

    @Column(name = "estimated_duration_min")
    private Integer estimatedDurationMin;

    @OneToMany(
            mappedBy = "day",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    private List<WorkoutDayExercise> exercises = new ArrayList<>();

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

    public void addExercise(WorkoutDayExercise item) {
        item.setDay(this);
        exercises.add(item);
    }

    public void removeExercise(WorkoutDayExercise item) {
        exercises.remove(item);
        item.setDay(null);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public WorkoutPlan getPlan() { return plan; }
    public void setPlan(WorkoutPlan plan) { this.plan = plan; }

    public short getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(short dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public WorkoutFocus getFocus() { return focus; }
    public void setFocus(WorkoutFocus focus) { this.focus = focus; }

    public Integer getEstimatedDurationMin() { return estimatedDurationMin; }
    public void setEstimatedDurationMin(Integer minutes) { this.estimatedDurationMin = minutes; }

    public List<WorkoutDayExercise> getExercises() { return exercises; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
