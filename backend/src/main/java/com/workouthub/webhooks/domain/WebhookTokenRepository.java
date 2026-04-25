package com.workouthub.webhooks.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookTokenRepository extends JpaRepository<WebhookToken, UUID> {

    Optional<WebhookToken> findByToken(String token);

    List<WebhookToken> findByUserIdAndPurpose(UUID userId, String purpose);

    Optional<WebhookToken> findByIdAndUserId(UUID id, UUID userId);
}
