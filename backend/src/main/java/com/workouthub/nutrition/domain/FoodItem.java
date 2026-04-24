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
@Table(name = "food_items")
public class FoodItem {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "name_tr", nullable = false, length = 120)
    private String nameTr;

    @Column(name = "name_en", nullable = false, length = 120)
    private String nameEn;

    @Column(name = "kcal_per_100g", nullable = false, precision = 6, scale = 1)
    private BigDecimal kcalPer100g;

    @Column(name = "protein_g", nullable = false, precision = 5, scale = 1)
    private BigDecimal proteinG;

    @Column(name = "carbs_g", nullable = false, precision = 5, scale = 1)
    private BigDecimal carbsG;

    @Column(name = "fat_g", nullable = false, precision = 5, scale = 1)
    private BigDecimal fatG;

    @Column(name = "default_serving_g", nullable = false, precision = 6, scale = 1)
    private BigDecimal defaultServingG;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getNameTr() { return nameTr; }
    public void setNameTr(String v) { this.nameTr = v; }
    public String getNameEn() { return nameEn; }
    public void setNameEn(String v) { this.nameEn = v; }
    public BigDecimal getKcalPer100g() { return kcalPer100g; }
    public void setKcalPer100g(BigDecimal v) { this.kcalPer100g = v; }
    public BigDecimal getProteinG() { return proteinG; }
    public void setProteinG(BigDecimal v) { this.proteinG = v; }
    public BigDecimal getCarbsG() { return carbsG; }
    public void setCarbsG(BigDecimal v) { this.carbsG = v; }
    public BigDecimal getFatG() { return fatG; }
    public void setFatG(BigDecimal v) { this.fatG = v; }
    public BigDecimal getDefaultServingG() { return defaultServingG; }
    public void setDefaultServingG(BigDecimal v) { this.defaultServingG = v; }
    public Instant getCreatedAt() { return createdAt; }
}
