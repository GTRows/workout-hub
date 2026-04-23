package com.workouthub.notifications;

import java.util.UUID;

public interface NotificationDispatcher {

    void send(UUID userId, String title, String body, String clickUrl);
}
