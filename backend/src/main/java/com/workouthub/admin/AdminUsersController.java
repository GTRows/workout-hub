package com.workouthub.admin;

import com.workouthub.admin.dto.AdminUserResponse;
import com.workouthub.admin.dto.CreateUserRequest;
import com.workouthub.admin.dto.UpdateUserRequest;
import com.workouthub.audit.AuditLogService;
import com.workouthub.auth.PasswordResetService;
import com.workouthub.common.security.AppUserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "Admin: Users", description = "ADMIN-only user management: list, create, update, delete.")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUsersController {

    private final AdminUsersService service;
    private final PasswordResetService passwordResetService;
    private final AuditLogService auditLog;

    public AdminUsersController(
            AdminUsersService service,
            PasswordResetService passwordResetService,
            AuditLogService auditLog) {
        this.service = service;
        this.passwordResetService = passwordResetService;
        this.auditLog = auditLog;
    }

    @PostMapping("/{id}/reset-link")
    public Map<String, String> resetLink(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        String token = passwordResetService.issueFor(id, principal.userId());
        auditLog.record(principal.userId(), "user.reset_link", "user", id, null);
        return Map.of("token", token);
    }

    @PostMapping
    public ResponseEntity<AdminUserResponse> create(
            @Valid @RequestBody CreateUserRequest req,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        AdminUserResponse body = service.create(req);
        auditLog.record(principal.userId(), "user.create", "user", body.id(),
                Map.of("email", body.email(), "role", body.role()));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping
    public List<AdminUserResponse> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public AdminUserResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public AdminUserResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest req,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        AdminUserResponse body = service.update(id, req);
        auditLog.record(principal.userId(), "user.update", "user", id, req);
        return body;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppUserPrincipal principal) {
        service.delete(id, principal.userId());
        auditLog.record(principal.userId(), "user.delete", "user", id, null);
        return ResponseEntity.noContent().build();
    }
}
