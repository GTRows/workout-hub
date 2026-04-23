package com.workouthub.push;

import com.workouthub.push.domain.PushSubscription;
import com.workouthub.push.domain.PushSubscriptionRepository;
import com.workouthub.push.dto.SubscribeRequest;
import com.workouthub.push.dto.SubscriptionDto;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PushService {

    private final PushSubscriptionRepository repo;

    public PushService(PushSubscriptionRepository repo) {
        this.repo = repo;
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
}
