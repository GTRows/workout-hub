package com.workouthub.notifications;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.workouthub.push.domain.PushSubscription;
import com.workouthub.push.domain.PushSubscriptionRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dispatcher that signs Web Push payloads with the configured VAPID
 * keypair and fans them out to every stored subscription for the target
 * user. Activated only when {@code app.push.vapid.private-key} is set;
 * otherwise the LoggingNotificationDispatcher remains in charge.
 */
@Component
@Primary
@ConditionalOnProperty(name = "app.push.vapid.private-key")
@Transactional
public class WebPushNotificationDispatcher implements NotificationDispatcher {

    private static final Logger log =
            LoggerFactory.getLogger(WebPushNotificationDispatcher.class);

    private final PushSubscriptionRepository subs;
    private final WebPushSender sender;
    private final ObjectMapper objectMapper;

    public WebPushNotificationDispatcher(
            PushSubscriptionRepository subs,
            WebPushSender sender,
            ObjectMapper objectMapper) {
        this.subs = subs;
        this.sender = sender;
        this.objectMapper = objectMapper;
    }

    @Override
    public void send(UUID userId, String title, String body, String clickUrl) {
        List<PushSubscription> targets = subs.findByUserId(userId);
        if (targets.isEmpty()) return;

        String payload = buildPayload(title, body, clickUrl);
        int gone = 0;
        int delivered = 0;
        for (PushSubscription sub : targets) {
            WebPushSender.Outcome outcome = sender.send(sub, payload);
            switch (outcome) {
                case OK -> delivered++;
                case GONE -> {
                    subs.delete(sub);
                    gone++;
                }
                case TRANSIENT_FAILURE -> {
                    // No-op; try again on next trigger.
                }
            }
        }
        log.info(
                "web-push dispatch userId={} delivered={} pruned={} subscriptions={}",
                userId, delivered, gone, targets.size());
    }

    private String buildPayload(String title, String body, String clickUrl) {
        try {
            return objectMapper.writeValueAsString(
                    Map.of("title", title, "body", body, "url", clickUrl));
        } catch (JacksonException ex) {
            throw new IllegalStateException("Failed to serialize push payload", ex);
        }
    }
}
