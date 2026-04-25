package com.workouthub.challenges.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonthlyChallengeRepository extends JpaRepository<MonthlyChallenge, UUID> {

    Optional<MonthlyChallenge> findByYearMonth(String yearMonth);
}
