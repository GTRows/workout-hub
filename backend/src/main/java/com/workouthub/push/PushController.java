package com.workouthub.push;

import com.workouthub.common.security.AppUserPrincipal;
import com.workouthub.push.dto.SubscribeRequest;
import com.workouthub.push.dto.SubscriptionDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/push")
@Tag(name = "Push Notifications", description = "Web Push subscription registration and notification delivery.")
public class PushController {

    private final PushService service;
    private final VapidConfig vapid;

    public PushController(PushService service, VapidConfig vapid) {
        this.service = service;
        this.vapid = vapid;
    }

    @GetMapping("/vapid-public-key")
    public Map<String, String> vapidPublicKey() {
        return Map.of("publicKey", vapid.publicKey() == null ? "" : vapid.publicKey());
    }

    @PostMapping("/subscribe")
    public ResponseEntity<SubscriptionDto> subscribe(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody SubscribeRequest request) {
        SubscriptionDto body = service.subscribe(principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @DeleteMapping("/subscribe")
    public ResponseEntity<Void> unsubscribe(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam("endpoint") String endpoint) {
        service.unsubscribe(principal.userId(), endpoint);
        return ResponseEntity.noContent().build();
    }
}
