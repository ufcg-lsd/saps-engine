package org.fogbowcloud.saps.engine.core.dispatcher.email.keystone;

import java.time.Instant;

public class IdentityToken {

    private final String accessId;
    private final Instant createdAt;
    private final Long duration;

    public IdentityToken(String accessId, String issuedAt, String expiresAt) {
        this.accessId = accessId;
        this.createdAt = Instant.now();
        this.duration = Instant.parse(expiresAt).getEpochSecond() - Instant.parse(issuedAt).getEpochSecond();
    }

    public String getAccessId() {
        return accessId;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(createdAt.plusMillis(duration));
    }
}
