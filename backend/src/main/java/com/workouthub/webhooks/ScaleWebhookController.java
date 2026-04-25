package com.workouthub.webhooks;

import com.workouthub.metrics.domain.BodyMetric;
import com.workouthub.metrics.domain.BodyMetricRepository;
import com.workouthub.webhooks.domain.WebhookToken;
import com.workouthub.webhooks.domain.WebhookTokenRepository;
import com.workouthub.webhooks.dto.ScalePayload;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks/scale")
@Transactional
public class ScaleWebhookController {

    private final WebhookTokenRepository tokenRepo;
    private final BodyMetricRepository metricRepo;

    public ScaleWebhookController(
            WebhookTokenRepository tokenRepo,
            BodyMetricRepository metricRepo) {
        this.tokenRepo = tokenRepo;
        this.metricRepo = metricRepo;
    }

    @PostMapping("/{token}")
    public ResponseEntity<Void> ingest(
            @PathVariable String token,
            @RequestBody ScalePayload payload) {
        WebhookToken t = tokenRepo.findByToken(token).orElse(null);
        if (t == null || !"scale".equals(t.getPurpose())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (payload == null || payload.weightKg() == null) {
            return ResponseEntity.badRequest().build();
        }
        Instant when = payload.timestamp() == null ? Instant.now() : payload.timestamp();
        LocalDate date = when.atZone(ZoneOffset.UTC).toLocalDate();

        if (metricRepo.findByUserIdAndRecordedDate(t.getUserId(), date).isPresent()) {
            t.setLastUsedAt(Instant.now());
            return ResponseEntity.status(HttpStatus.OK).build();
        }
        BodyMetric m = new BodyMetric();
        m.setUserId(t.getUserId());
        m.setRecordedDate(date);
        m.setWeightKg(payload.weightKg());
        metricRepo.save(m);
        t.setLastUsedAt(Instant.now());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
