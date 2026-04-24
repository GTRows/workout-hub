package com.workouthub.twofa;

import com.workouthub.common.web.NotFoundException;
import com.workouthub.twofa.domain.UserTotp;
import com.workouthub.twofa.domain.UserTotpRepository;
import com.workouthub.twofa.dto.SetupResponse;
import com.workouthub.twofa.dto.StatusResponse;
import com.workouthub.users.domain.User;
import com.workouthub.users.domain.UserRepository;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TwoFactorService {

    private static final String ISSUER = "WorkoutHub";
    private static final int DIGITS = 6;
    private static final int PERIOD_SECONDS = 30;

    private final UserTotpRepository repo;
    private final UserRepository users;
    private final SecretGenerator secretGenerator;
    private final CodeVerifier verifier;

    public TwoFactorService(UserTotpRepository repo, UserRepository users) {
        this(repo, users,
                new DefaultSecretGenerator(),
                new SystemTimeProvider(),
                new DefaultCodeGenerator(HashingAlgorithm.SHA1, DIGITS));
    }

    TwoFactorService(
            UserTotpRepository repo,
            UserRepository users,
            SecretGenerator secretGenerator,
            TimeProvider timeProvider,
            CodeGenerator codeGenerator) {
        this.repo = repo;
        this.users = users;
        this.secretGenerator = secretGenerator;
        DefaultCodeVerifier v = new DefaultCodeVerifier(codeGenerator, timeProvider);
        v.setAllowedTimePeriodDiscrepancy(1);
        this.verifier = v;
    }

    @Transactional(readOnly = true)
    public StatusResponse status(UUID userId) {
        return repo.findById(userId)
                .map(t -> new StatusResponse(true, t.isEnabled()))
                .orElse(new StatusResponse(false, false));
    }

    public SetupResponse beginSetup(UUID userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        UserTotp totp = repo.findById(userId).orElseGet(() -> {
            UserTotp t = new UserTotp();
            t.setUserId(userId);
            t.setEnabled(false);
            return t;
        });

        if (!totp.isEnabled()) {
            totp.setSecretBase32(secretGenerator.generate());
        }
        repo.save(totp);

        QrData data = new QrData.Builder()
                .label(user.getEmail())
                .secret(totp.getSecretBase32())
                .issuer(ISSUER)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(DIGITS)
                .period(PERIOD_SECONDS)
                .build();

        return new SetupResponse(totp.getSecretBase32(), data.getUri());
    }

    public boolean verifyAndEnable(UUID userId, String code) {
        UserTotp totp = repo.findById(userId)
                .orElseThrow(() -> new NotFoundException("No TOTP setup for user"));
        if (!verifier.isValidCode(totp.getSecretBase32(), code)) {
            return false;
        }
        totp.setEnabled(true);
        return true;
    }

    public void disable(UUID userId) {
        repo.findById(userId).ifPresent(t -> {
            t.setEnabled(false);
            repo.save(t);
        });
    }

    public boolean verifyCode(UUID userId, String code) {
        return repo.findById(userId)
                .filter(UserTotp::isEnabled)
                .map(t -> verifier.isValidCode(t.getSecretBase32(), code))
                .orElse(false);
    }

    public boolean isEnabled(UUID userId) {
        return repo.findById(userId).map(UserTotp::isEnabled).orElse(false);
    }
}
