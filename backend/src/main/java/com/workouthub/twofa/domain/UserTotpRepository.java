package com.workouthub.twofa.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTotpRepository extends JpaRepository<UserTotp, UUID> {}
