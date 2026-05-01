package com.openrecords.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Email configuration loaded from application.yml under
 * the `openrecords.email` prefix.
 */
@Configuration
@ConfigurationProperties(prefix = "openrecords.email")
public class EmailProperties {

    private String from;
    private String fromName;
    private String frontendBaseUrl;

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }

    public String getFrontendBaseUrl() { return frontendBaseUrl; }
    public void setFrontendBaseUrl(String frontendBaseUrl) { this.frontendBaseUrl = frontendBaseUrl; }
}