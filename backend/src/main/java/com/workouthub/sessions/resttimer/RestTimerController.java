package com.workouthub.sessions.resttimer;

import com.workouthub.common.security.AppUserPrincipal;
import com.workouthub.sessions.resttimer.dto.RestTimerScheduleDto;
import com.workouthub.sessions.resttimer.dto.ScheduleRestTimerRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions/{sessionId}/rest-timer")
@Tag(
        name = "Rest Timer",
        description =
                "Server-side schedule for rest-timer push notifications. The browser posts a"
                        + " schedule when a rest interval starts; the backend dispatches a Web Push"
                        + " when it elapses, even if the tab is backgrounded.")
public class RestTimerController {

    private final RestTimerScheduleService service;

    public RestTimerController(RestTimerScheduleService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(operationId = "scheduleRestTimer")
    public ResponseEntity<RestTimerScheduleDto> schedule(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID sessionId,
            @Valid @RequestBody ScheduleRestTimerRequest req) {
        RestTimerSchedule row = service.schedule(
                principal.userId(),
                sessionId,
                req.seconds(),
                req.title(),
                req.body(),
                req.clickUrl());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RestTimerScheduleDto.from(row));
    }

    @DeleteMapping
    @Operation(operationId = "cancelRestTimer")
    public ResponseEntity<Void> cancel(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID sessionId) {
        service.cancel(principal.userId(), sessionId);
        return ResponseEntity.noContent().build();
    }
}
