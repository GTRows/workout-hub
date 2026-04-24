package com.workouthub.twofa;

import com.workouthub.common.security.AppUserPrincipal;
import com.workouthub.twofa.dto.SetupResponse;
import com.workouthub.twofa.dto.StatusResponse;
import com.workouthub.twofa.dto.VerifyRequest;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/2fa")
public class TwoFactorController {

    private final TwoFactorService service;

    public TwoFactorController(TwoFactorService service) {
        this.service = service;
    }

    @GetMapping
    public StatusResponse status(@AuthenticationPrincipal AppUserPrincipal principal) {
        return service.status(principal.userId());
    }

    @PostMapping("/setup")
    public SetupResponse setup(@AuthenticationPrincipal AppUserPrincipal principal) {
        return service.beginSetup(principal.userId());
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody VerifyRequest req) {
        boolean ok = service.verifyAndEnable(principal.userId(), req.code());
        if (!ok) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("enabled", false));
        }
        return ResponseEntity.ok(Map.of("enabled", true));
    }

    @DeleteMapping
    public ResponseEntity<Void> disable(@AuthenticationPrincipal AppUserPrincipal principal) {
        service.disable(principal.userId());
        return ResponseEntity.noContent().build();
    }
}
