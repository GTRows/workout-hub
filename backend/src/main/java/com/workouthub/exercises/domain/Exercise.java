package com.workouthub.exercises.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "exercises")
public class Exercise {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "name_tr", nullable = false, length = 120)
    private String nameTr;

    @Column(name = "name_en", nullable = false, length = 120)
    private String nameEn;

    @Column(nullable = false, length = 20)
    private Category category;

    @Column(nullable = false, length = 20)
    private Equipment equipment;

    @Column(name = "muscle_primary", nullable = false, length = 40)
    private String musclePrimary;

    @Column(name = "muscle_secondary", length = 40)
    private String muscleSecondary;

    @Column(name = "description_tr", columnDefinition = "TEXT")
    private String descriptionTr;

    @Column(name = "description_en", columnDefinition = "TEXT")
    private String descriptionEn;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "form_tips_tr", columnDefinition = "text[]")
    private List<String> formTipsTr = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "form_tips_en", columnDefinition = "text[]")
    private List<String> formTipsEn = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "common_mistakes_tr", columnDefinition = "text[]")
    private List<String> commonMistakesTr = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "common_mistakes_en", columnDefinition = "text[]")
    private List<String> commonMistakesEn = new ArrayList<>();

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @Column(nullable = false, length = 20)
    private Difficulty difficulty;

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

    public String getNameTr() { return nameTr; }
    public void setNameTr(String nameTr) { this.nameTr = nameTr; }

    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public Equipment getEquipment() { return equipment; }
    public void setEquipment(Equipment equipment) { this.equipment = equipment; }

    public String getMusclePrimary() { return musclePrimary; }
    public void setMusclePrimary(String musclePrimary) { this.musclePrimary = musclePrimary; }

    public String getMuscleSecondary() { return muscleSecondary; }
    public void setMuscleSecondary(String muscleSecondary) { this.muscleSecondary = muscleSecondary; }

    public String getDescriptionTr() { return descriptionTr; }
    public void setDescriptionTr(String descriptionTr) { this.descriptionTr = descriptionTr; }

    public String getDescriptionEn() { return descriptionEn; }
    public void setDescriptionEn(String descriptionEn) { this.descriptionEn = descriptionEn; }

    public List<String> getFormTipsTr() { return formTipsTr; }
    public void setFormTipsTr(List<String> formTipsTr) { this.formTipsTr = formTipsTr; }

    public List<String> getFormTipsEn() { return formTipsEn; }
    public void setFormTipsEn(List<String> formTipsEn) { this.formTipsEn = formTipsEn; }

    public List<String> getCommonMistakesTr() { return commonMistakesTr; }
    public void setCommonMistakesTr(List<String> commonMistakesTr) { this.commonMistakesTr = commonMistakesTr; }

    public List<String> getCommonMistakesEn() { return commonMistakesEn; }
    public void setCommonMistakesEn(List<String> commonMistakesEn) { this.commonMistakesEn = commonMistakesEn; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
