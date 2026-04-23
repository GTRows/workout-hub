package com.workouthub.notifications;

import com.workouthub.push.VapidConfig;
import com.workouthub.push.domain.PushSubscription;
import java.security.Security;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Real Web Push sender backed by nl.martijndwars:web-push. Only loaded
 * when {@code app.push.vapid.private-key} is non-empty; otherwise the
 * LoggingNotificationDispatcher stays the only NotificationDispatcher
 * bean and the scheduler degrades to logs.
 */
@Component
@ConditionalOnProperty(name = "app.push.vapid.private-key")
public class WebPushJavaSender implements WebPushSender {

    private static final Logger log = LoggerFactory.getLogger(WebPushJavaSender.class);

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private final PushService push;

    public WebPushJavaSender(VapidConfig vapid) {
        try {
            this.push = new PushService(vapid.publicKey(), vapid.privateKey(), vapid.subject());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to initialize PushService", ex);
        }
    }

    @Override
    public Outcome send(PushSubscription sub, String jsonPayload) {
        try {
            Subscription.Keys keys = new Subscription.Keys(sub.getP256dhKey(), sub.getAuthKey());
            Subscription remote = new Subscription(sub.getEndpoint(), keys);
            Notification notification = new Notification(remote, jsonPayload);
            HttpResponse response = push.send(notification);
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) return Outcome.OK;
            if (status == 404 || status == 410) return Outcome.GONE;
            log.warn("web-push transient failure endpoint={} status={}",
                    sub.getEndpoint(), status);
            return Outcome.TRANSIENT_FAILURE;
        } catch (Exception ex) {
            log.warn("web-push exception endpoint={} message={}",
                    sub.getEndpoint(), ex.getMessage());
            return Outcome.TRANSIENT_FAILURE;
        }
    }
}
