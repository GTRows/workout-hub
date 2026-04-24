package com.workouthub.nutrition.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "nutrition_entries")
public class NutritionEntry {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "food_id", nullable = false)
    private UUID foodId;

    @Column(name = "serving_g", nullable = false, precision = 6, scale = 1)
    private BigDecimal servingG;

    @Column(name = "consumed_at", nullable = false)
    private Instant consumedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID v) { this.userId = v; }
    public UUID getFoodId() { return foodId; }
    public void setFoodId(UUID v) { this.foodId = v; }
    public BigDecimal getServingG() { return servingG; }
    public void setServingG(BigDecimal v) { this.servingG = v; }
    public Instant getConsumedAt() { return consumedAt; }
    public void setConsumedAt(Instant v) { this.consumedAt = v; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getCreatedAt() { return createdAt; }
}
