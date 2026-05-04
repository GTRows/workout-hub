package com.workouthub.sessions.domain;

import com.workouthub.exercises.domain.Exercise;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "session_sets")
public class SessionSet {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private WorkoutSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Column(name = "set_number", nullable = false)
    private short setNumber;

    @Column(name = "reps_done", nullable = false)
    private short repsDone;

    @Column(name = "weight_kg", precision = 6, scale = 2)
    private BigDecimal weightKg;

    @Column
    private Short rpe;

    @Column(nullable = false)
    private boolean completed = true;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "client_set_id")
    private UUID clientSetId;

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

    public WorkoutSession getSession() { return session; }
    public void setSession(WorkoutSession session) { this.session = session; }

    public Exercise getExercise() { return exercise; }
    public void setExercise(Exercise exercise) { this.exercise = exercise; }

    public short getSetNumber() { return setNumber; }
    public void setSetNumber(short setNumber) { this.setNumber = setNumber; }

    public short getRepsDone() { return repsDone; }
    public void setRepsDone(short repsDone) { this.repsDone = repsDone; }

    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }

    public Short getRpe() { return rpe; }
    public void setRpe(Short rpe) { this.rpe = rpe; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public UUID getClientSetId() { return clientSetId; }
    public void setClientSetId(UUID clientSetId) { this.clientSetId = clientSetId; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
