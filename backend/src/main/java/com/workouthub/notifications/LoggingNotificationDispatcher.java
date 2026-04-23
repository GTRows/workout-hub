package com.workouthub.notifications;

import com.workouthub.push.domain.PushSubscription;
import com.workouthub.push.domain.PushSubscriptionRepository;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Placeholder sender: logs what it would have delivered and lists the
 * push subscriptions that would receive it. A real Web Push implementation
 * swaps this bean out once the webpush-java dependency is approved.
 */
@Component
public class LoggingNotificationDispatcher implements NotificationDispatcher {

    private static final Logger log =
            LoggerFactory.getLogger(LoggingNotificationDispatcher.class);

    private final PushSubscriptionRepository subs;

    @Autowired
    public LoggingNotificationDispatcher(PushSubscriptionRepository subs) {
        this.subs = subs;
    }

    @Override
    public void send(UUID userId, String title, String body, String clickUrl) {
        List<PushSubscription> targets = subs.findByUserId(userId);
        log.info(
                "notification (logging dispatcher) userId={} title='{}' body='{}' url='{}' recipients={}",
                userId, title, body, clickUrl, targets.size());
    }
}
