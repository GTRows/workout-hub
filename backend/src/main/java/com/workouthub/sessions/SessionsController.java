package com.workouthub.sessions;

import com.workouthub.common.security.AppUserPrincipal;
import com.workouthub.sessions.dto.FinishSessionRequest;
import com.workouthub.sessions.dto.SessionDto;
import com.workouthub.sessions.dto.StartSessionRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions")
public class SessionsController {

    private final SessionsService service;

    public SessionsController(SessionsService service) {
        this.service = service;
    }

    @PostMapping("/start")
    public ResponseEntity<SessionDto> start(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestBody(required = false) StartSessionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.start(principal.userId(), req));
    }

    @GetMapping("/active")
    public ResponseEntity<SessionDto> active(
            @AuthenticationPrincipal AppUserPrincipal principal) {
        return service.getActive(principal.userId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/{id}/finish")
    public SessionDto finish(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) FinishSessionRequest req) {
        return service.finish(principal.userId(), id, req);
    }
}
