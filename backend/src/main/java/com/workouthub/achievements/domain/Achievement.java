package com.workouthub.achievements.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "achievements")
public class Achievement {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = "name_tr", nullable = false)
    private String nameTr;

    @Column(name = "name_en", nullable = false)
    private String nameEn;

    @Column(name = "description_tr", columnDefinition = "TEXT")
    private String descriptionTr;

    @Column(name = "description_en", columnDefinition = "TEXT")
    private String descriptionEn;

    @Column(length = 32)
    private String icon;

    @Column(name = "rule_type", nullable = false, length = 32)
    private String ruleType;

    @Column(nullable = false)
    private int threshold;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getNameTr() { return nameTr; }
    public String getNameEn() { return nameEn; }
    public String getDescriptionTr() { return descriptionTr; }
    public String getDescriptionEn() { return descriptionEn; }
    public String getIcon() { return icon; }
    public String getRuleType() { return ruleType; }
    public int getThreshold() { return threshold; }
    public Instant getCreatedAt() { return createdAt; }
}
