package com.workouthub.sessions;

import com.workouthub.common.security.AppUserPrincipal;
import com.workouthub.sessions.SessionSetsService.AddSetResult;
import com.workouthub.sessions.dto.AddSetRequest;
import com.workouthub.sessions.dto.SessionSetDto;
import com.workouthub.sessions.dto.UpdateSetRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions/{sessionId}/sets")
@Tag(name = "Workout Sessions: Sets", description = "Per-set logging within an active workout session.")
public class SessionSetsController {

    private final SessionSetsService service;

    public SessionSetsController(SessionSetsService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<SessionSetDto> add(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID sessionId,
            @Valid @RequestBody AddSetRequest req) {
        AddSetResult result = service.add(principal.userId(), sessionId, req);
        HttpStatus status = result.idempotentHit() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(result.dto());
    }

    @PutMapping("/{setId}")
    public SessionSetDto update(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID sessionId,
            @PathVariable UUID setId,
            @Valid @RequestBody UpdateSetRequest req) {
        return service.update(principal.userId(), sessionId, setId, req);
    }

    @DeleteMapping("/{setId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID sessionId,
            @PathVariable UUID setId) {
        service.delete(principal.userId(), sessionId, setId);
        return ResponseEntity.noContent().build();
    }
}
