package com.workouthub.challenges;

import static com.workouthub.challenges.ChallengesController.toDto;

import com.workouthub.challenges.domain.MonthlyChallenge;
import com.workouthub.challenges.domain.MonthlyChallengeRepository;
import com.workouthub.challenges.dto.MonthlyChallengeDto;
import com.workouthub.challenges.dto.UpsertChallengeRequest;
import com.workouthub.common.web.NotFoundException;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/challenges")
@Tag(name = "Admin: Challenges", description = "ADMIN-only monthly-challenge management and configuration.")
@PreAuthorize("hasRole('ADMIN')")
@Transactional
public class AdminChallengesController {

    private final MonthlyChallengeRepository repo;

    public AdminChallengesController(MonthlyChallengeRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<MonthlyChallengeDto> list() {
        return repo.findAll().stream()
                .sorted((a, b) -> b.getYearMonth().compareTo(a.getYearMonth()))
                .map(ChallengesController::toDto)
                .toList();
    }

    @PostMapping
    public ResponseEntity<MonthlyChallengeDto> create(
            @Valid @RequestBody UpsertChallengeRequest req) {
        MonthlyChallenge ch = repo.findByYearMonth(req.yearMonth()).orElseGet(MonthlyChallenge::new);
        applyTo(ch, req);
        MonthlyChallenge saved = repo.save(ch);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @PutMapping("/{id}")
    public MonthlyChallengeDto update(
            @PathVariable UUID id,
            @Valid @RequestBody UpsertChallengeRequest req) {
        MonthlyChallenge ch = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Challenge not found: " + id));
        applyTo(ch, req);
        return toDto(ch);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        MonthlyChallenge ch = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Challenge not found: " + id));
        repo.delete(ch);
        return ResponseEntity.noContent().build();
    }

    private static void applyTo(MonthlyChallenge ch, UpsertChallengeRequest req) {
        ch.setYearMonth(req.yearMonth());
        ch.setNameTr(req.nameTr());
        ch.setNameEn(req.nameEn());
        ch.setDescriptionTr(req.descriptionTr());
        ch.setDescriptionEn(req.descriptionEn());
        ch.setRuleType(req.ruleType());
        ch.setThreshold(req.threshold());
    }
}
