package com.workouthub.webhooks;

import com.workouthub.common.security.AppUserPrincipal;
import com.workouthub.common.web.NotFoundException;
import com.workouthub.webhooks.domain.WebhookToken;
import com.workouthub.webhooks.domain.WebhookTokenRepository;
import com.workouthub.webhooks.dto.WebhookTokenDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/webhook-tokens")
@Tag(name = "Webhook Tokens", description = "Per-user HMAC tokens for inbound webhook integrations.")
@Transactional
public class WebhookTokensController {

    private static final SecureRandom RNG = new SecureRandom();

    private final WebhookTokenRepository repo;

    public WebhookTokensController(WebhookTokenRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<WebhookTokenDto> list(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam(value = "purpose", required = false) String purpose) {
        List<WebhookToken> tokens = purpose == null
                ? repo.findByUserIdAndPurpose(principal.userId(), "scale")
                : repo.findByUserIdAndPurpose(principal.userId(), purpose);
        return tokens.stream().map(this::toDto).toList();
    }

    @PostMapping
    public ResponseEntity<WebhookTokenDto> mint(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam(value = "purpose", defaultValue = "scale") String purpose) {
        WebhookToken t = new WebhookToken();
        t.setUserId(principal.userId());
        t.setPurpose(purpose);
        t.setToken(generateToken());
        WebhookToken saved = repo.save(t);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id) {
        WebhookToken t = repo.findByIdAndUserId(id, principal.userId())
                .orElseThrow(() -> new NotFoundException("Token not found: " + id));
        repo.delete(t);
        return ResponseEntity.noContent().build();
    }

    private static String generateToken() {
        byte[] raw = new byte[32];
        RNG.nextBytes(raw);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }

    private WebhookTokenDto toDto(WebhookToken t) {
        return new WebhookTokenDto(
                t.getId(), t.getToken(), t.getPurpose(),
                t.getCreatedAt(), t.getLastUsedAt());
    }
}
