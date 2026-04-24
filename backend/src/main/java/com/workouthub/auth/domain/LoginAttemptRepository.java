package com.workouthub.auth.domain;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, UUID> {

    @Query("""
            SELECT COUNT(a) FROM LoginAttempt a
            WHERE LOWER(a.email) = LOWER(:email)
              AND a.success = false
              AND a.attemptedAt >= :since
            """)
    long countFailuresSince(@Param("email") String email, @Param("since") Instant since);
}
