package com.ratel.fm.config.web;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * HTTPS deployment connector settings.
 *
 * <p>The portable package uses the main Spring Boot port for HTTPS and can keep a
 * separate HTTP connector for ordinary LAN access.</p>
 */
@Component
@ConfigurationProperties(prefix = "app.https")
public class HttpsConnectorProperties {

    /** Whether to keep an additional HTTP connector while the main server port is HTTPS. */
    private boolean httpEnabled = false;

    /** Additional HTTP connector port. */
    private int httpPort = 38000;

    /**
     * Return whether the additional HTTP connector should be registered.
     */
    public boolean isHttpEnabled() {
        return httpEnabled;
    }

    /**
     * Set whether the additional HTTP connector should be registered.
     */
    public void setHttpEnabled(boolean httpEnabled) {
        this.httpEnabled = httpEnabled;
    }

    /**
     * Return the additional HTTP connector port.
     */
    public int getHttpPort() {
        return httpPort;
    }

    /**
     * Set the additional HTTP connector port.
     */
    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }
}
