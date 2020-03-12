package org.fogbowcloud.saps.engine.core.dispatcher.email.keystone;

public class IdentityToken {

    private final String accessId;
    private final String issuedAt;
    private final String expiresAt;

    public IdentityToken(String accessId, String issuedAt, String expiresAt) {
        this.accessId = accessId;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    public String getAccessId() {
        return accessId;
    }

    public String getIssuedAt() {
        return issuedAt;
    }

    public String getExpiresAt() {
        return expiresAt;
    }
}
