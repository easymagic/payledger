package com.payledger.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payledger.security.jwt")
public class JwtProperties {

    /**
     * HMAC secret for demo JWTs. Override via JWT_SECRET in any real deployment.
     */
    private String secret = "payledger-dev-secret-change-me-please-32bytes-min";
    private long expiryMinutes = 120;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpiryMinutes() {
        return expiryMinutes;
    }

    public void setExpiryMinutes(long expiryMinutes) {
        this.expiryMinutes = expiryMinutes;
    }
}
