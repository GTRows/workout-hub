package com.workouthub.supplements.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplementRepository extends JpaRepository<Supplement, UUID> {

    List<Supplement> findByUserIdOrderByCreatedAtAsc(UUID userId);

    Optional<Supplement> findByIdAndUserId(UUID id, UUID userId);
}
