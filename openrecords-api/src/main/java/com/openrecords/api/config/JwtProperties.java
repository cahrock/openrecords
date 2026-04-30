package com.openrecords.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT-related configuration loaded from application.yml under
 * the `openrecords.jwt` prefix.
 */
@Configuration
@ConfigurationProperties(prefix = "openrecords.jwt")
public class JwtProperties {

    private String secret;
    private long accessTokenMs;
    private long refreshTokenMs;
    private String issuer;

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public long getAccessTokenMs() { return accessTokenMs; }
    public void setAccessTokenMs(long accessTokenMs) { this.accessTokenMs = accessTokenMs; }

    public long getRefreshTokenMs() { return refreshTokenMs; }
    public void setRefreshTokenMs(long refreshTokenMs) { this.refreshTokenMs = refreshTokenMs; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
}