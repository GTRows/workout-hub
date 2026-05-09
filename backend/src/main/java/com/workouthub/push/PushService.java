package com.workouthub.push;

import com.workouthub.notifications.NotificationDispatcher;
import com.workouthub.push.domain.PushSubscription;
import com.workouthub.push.domain.PushSubscriptionRepository;
import com.workouthub.push.dto.PushTestResponse;
import com.workouthub.push.dto.SubscribeRequest;
import com.workouthub.push.dto.SubscriptionDto;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PushService {

    private final PushSubscriptionRepository repo;
    private final NotificationDispatcher dispatcher;

    public PushService(PushSubscriptionRepository repo, NotificationDispatcher dispatcher) {
        this.repo = repo;
        this.dispatcher = dispatcher;
    }

    public SubscriptionDto subscribe(UUID userId, SubscribeRequest req) {
        PushSubscription sub = repo.findByEndpoint(req.endpoint()).orElseGet(PushSubscription::new);
        sub.setUserId(userId);
        sub.setEndpoint(req.endpoint());
        sub.setP256dhKey(req.keys().p256dh());
        sub.setAuthKey(req.keys().auth());
        sub.setUserAgent(req.userAgent());
        PushSubscription saved = repo.save(sub);
        return new SubscriptionDto(
                saved.getId(), saved.getEndpoint(), saved.getCreatedAt(), saved.getUpdatedAt());
    }

    public void unsubscribe(UUID userId, String endpoint) {
        repo.findByEndpoint(endpoint).ifPresent(sub -> {
            if (sub.getUserId().equals(userId)) {
                repo.delete(sub);
            }
        });
    }

    public PushTestResponse sendSelfTest(UUID userId) {
        int subs = repo.findByUserId(userId).size();
        if (subs == 0) {
            return new PushTestResponse(0, 0);
        }
        dispatcher.send(
                userId,
                "WorkoutHub test reminder",
                "If you can read this, push notifications are working.",
                "/profile");
        return new PushTestResponse(subs, subs);
    }
}
