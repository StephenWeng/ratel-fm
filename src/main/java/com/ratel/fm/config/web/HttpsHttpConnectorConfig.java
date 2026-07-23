package com.ratel.fm.config.web;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Adds an optional plain HTTP connector when the primary embedded Tomcat port is HTTPS.
 *
 * <p>This keeps existing LAN HTTP access available while browsers can use the HTTPS
 * entry for microphone and geolocation permissions.</p>
 */
@Configuration
public class HttpsHttpConnectorConfig {

    /**
     * Register an additional HTTP connector when configured by the portable startup script.
     */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> additionalHttpConnectorCustomizer(
            HttpsConnectorProperties properties) {
        return factory -> {
            if (!properties.isHttpEnabled() || properties.getHttpPort() <= 0) {
                return;
            }
            Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
            connector.setScheme("http");
            connector.setSecure(false);
            connector.setPort(properties.getHttpPort());
            factory.addAdditionalConnectors(connector);
        };
    }
}
