package com.workouthub.push;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class VapidConfig {

    private final String publicKey;
    private final String privateKey;
    private final String subject;

    public VapidConfig(
            @Value("${app.push.vapid.public-key:}") String publicKey,
            @Value("${app.push.vapid.private-key:}") String privateKey,
            @Value("${app.push.vapid.subject:mailto:admin@localhost}") String subject) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.subject = subject;
    }

    public String publicKey() { return publicKey; }
    public String privateKey() { return privateKey; }
    public String subject() { return subject; }
}
