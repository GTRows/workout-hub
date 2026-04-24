package com.workouthub.twofa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_totp")
public class UserTotp {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "secret_base32", nullable = false, length = 64)
    private String secretBase32;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "backup_codes_hash", columnDefinition = "TEXT")
    private String backupCodesHash;

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

    public String getSecretBase32() { return secretBase32; }
    public void setSecretBase32(String v) { this.secretBase32 = v; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getBackupCodesHash() { return backupCodesHash; }
    public void setBackupCodesHash(String v) { this.backupCodesHash = v; }
}
