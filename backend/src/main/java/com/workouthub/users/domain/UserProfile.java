package com.workouthub.users.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "user_profile")
public class UserProfile {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "height_cm")
    private Integer heightCm;

    @Column(name = "weight_kg", precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(length = 20)
    private String gender;

    @Column(name = "health_notes", columnDefinition = "TEXT")
    private String healthNotes;

    @Column(columnDefinition = "TEXT")
    private String goals;

    @Column(name = "daily_kcal_goal")
    private Integer dailyKcalGoal;

    @Column(name = "daily_protein_g_goal")
    private Integer dailyProteinGGoal;

    @Column(name = "daily_carbs_g_goal")
    private Integer dailyCarbsGGoal;

    @Column(name = "daily_fat_g_goal")
    private Integer dailyFatGGoal;

    @Column(name = "streak_freeze_used_month", length = 7)
    private String streakFreezeUsedMonth;

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

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Integer getHeightCm() { return heightCm; }
    public void setHeightCm(Integer heightCm) { this.heightCm = heightCm; }

    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getHealthNotes() { return healthNotes; }
    public void setHealthNotes(String healthNotes) { this.healthNotes = healthNotes; }

    public String getGoals() { return goals; }
    public void setGoals(String goals) { this.goals = goals; }

    public Integer getDailyKcalGoal() { return dailyKcalGoal; }
    public void setDailyKcalGoal(Integer v) { this.dailyKcalGoal = v; }

    public Integer getDailyProteinGGoal() { return dailyProteinGGoal; }
    public void setDailyProteinGGoal(Integer v) { this.dailyProteinGGoal = v; }

    public Integer getDailyCarbsGGoal() { return dailyCarbsGGoal; }
    public void setDailyCarbsGGoal(Integer v) { this.dailyCarbsGGoal = v; }

    public Integer getDailyFatGGoal() { return dailyFatGGoal; }
    public void setDailyFatGGoal(Integer v) { this.dailyFatGGoal = v; }

    public String getStreakFreezeUsedMonth() { return streakFreezeUsedMonth; }
    public void setStreakFreezeUsedMonth(String v) { this.streakFreezeUsedMonth = v; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
