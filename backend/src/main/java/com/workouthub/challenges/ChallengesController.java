package com.workouthub.challenges;

import com.workouthub.challenges.domain.MonthlyChallenge;
import com.workouthub.challenges.domain.MonthlyChallengeRepository;
import com.workouthub.challenges.dto.ChallengeProgressDto;
import com.workouthub.challenges.dto.MonthlyChallengeDto;
import com.workouthub.common.security.AppUserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/challenges")
@Tag(name = "Challenges", description = "Active monthly challenges and the current user's enrollments.")
public class ChallengesController {

    private final MonthlyChallengeRepository repo;
    private final ChallengeProgressService progress;
    private final Clock clock;

    public ChallengesController(
            MonthlyChallengeRepository repo,
            ChallengeProgressService progress,
            Clock clock) {
        this.repo = repo;
        this.progress = progress;
        this.clock = clock;
    }

    @GetMapping("/current")
    @Transactional(readOnly = true)
    public ResponseEntity<ChallengeProgressDto> current(
            @AuthenticationPrincipal AppUserPrincipal principal) {
        String ym = YearMonth.from(LocalDate.now(clock)).toString();
        MonthlyChallenge ch = repo.findByYearMonth(ym).orElse(null);
        if (ch == null) return ResponseEntity.noContent().build();

        int currentValue = progress.compute(ch, principal.userId());
        return ResponseEntity.ok(new ChallengeProgressDto(
                toDto(ch), currentValue, currentValue >= ch.getThreshold()));
    }

    static MonthlyChallengeDto toDto(MonthlyChallenge ch) {
        return new MonthlyChallengeDto(
                ch.getId(),
                ch.getYearMonth(),
                ch.getNameTr(),
                ch.getNameEn(),
                ch.getDescriptionTr(),
                ch.getDescriptionEn(),
                ch.getRuleType(),
                ch.getThreshold());
    }
}
