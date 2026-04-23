package com.workouthub.supplements;

import com.workouthub.common.security.AppUserPrincipal;
import com.workouthub.supplements.dto.CreateSupplementRequest;
import com.workouthub.supplements.dto.SupplementDto;
import com.workouthub.supplements.dto.UpdateSupplementRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/supplements")
public class SupplementsController {

    private final SupplementsService service;

    public SupplementsController(SupplementsService service) {
        this.service = service;
    }

    @GetMapping
    public List<SupplementDto> list(@AuthenticationPrincipal AppUserPrincipal principal) {
        return service.list(principal.userId());
    }

    @PostMapping
    public ResponseEntity<SupplementDto> create(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CreateSupplementRequest req) {
        SupplementDto body = service.create(principal.userId(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PutMapping("/{id}")
    public SupplementDto update(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSupplementRequest req) {
        return service.update(principal.userId(), id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id) {
        service.delete(principal.userId(), id);
        return ResponseEntity.noContent().build();
    }
}
