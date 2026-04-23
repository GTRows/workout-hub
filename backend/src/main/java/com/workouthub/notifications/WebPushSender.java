package com.workouthub.notifications;

import com.workouthub.push.domain.PushSubscription;

/**
 * Thin seam over the web-push HTTP call so unit tests can swap a fake
 * implementation in without standing up a real push endpoint.
 */
public interface WebPushSender {

    enum Outcome {
        /** Delivered (2xx). */
        OK,
        /** Subscription is permanently gone; the row must be pruned. */
        GONE,
        /** Any other error; retried on the next trigger. */
        TRANSIENT_FAILURE
    }

    Outcome send(PushSubscription subscription, String jsonPayload);
}
