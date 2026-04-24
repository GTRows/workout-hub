package com.workouthub.auth;

import com.workouthub.auth.dto.AuthResponse;
import com.workouthub.auth.dto.LoginRequest;
import com.workouthub.auth.dto.RefreshRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest req,
            HttpServletRequest httpReq) {
        return authService.login(req, httpReq.getHeader("User-Agent"));
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(
            @Valid @RequestBody RefreshRequest req,
            HttpServletRequest httpReq) {
        return authService.refresh(req, httpReq.getHeader("User-Agent"));
    }
}
