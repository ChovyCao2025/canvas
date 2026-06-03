package org.chovy.canvas.config;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("prod")
public class ProductionConfigGuard implements SmartInitializingSingleton {

    private static final String DEFAULT_EVENT_REPORT_SECRET = "canvas-event-report-secret-2026!!";
    private static final String DEFAULT_SECRET_CIPHER_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    private final List<String> allowedOrigins;
    private final boolean allowCredentials;
    private final String eventReportSecret;
    private final String jwtSecret;
    private final String datasourceUsername;
    private final String datasourcePassword;
    private final String secretCipherKey;

    public ProductionConfigGuard(
            @Value("${canvas.cors.allowed-origins:}") List<String> allowedOrigins,
            @Value("${canvas.cors.allow-credentials:true}") boolean allowCredentials,
            @Value("${canvas.events.report-secret:}") String eventReportSecret,
            @Value("${canvas.jwt.secret:}") String jwtSecret,
            @Value("${spring.datasource.username:}") String datasourceUsername,
            @Value("${spring.datasource.password:}") String datasourcePassword,
            @Value("${canvas.secret-cipher.key:}") String secretCipherKey) {
        this.allowedOrigins = allowedOrigins;
        this.allowCredentials = allowCredentials;
        this.eventReportSecret = eventReportSecret;
        this.jwtSecret = jwtSecret;
        this.datasourceUsername = datasourceUsername;
        this.datasourcePassword = datasourcePassword;
        this.secretCipherKey = secretCipherKey;
    }

    @Override
    public void afterSingletonsInstantiated() {
        validate();
    }

    void validate() {
        if (allowCredentials && containsWildcardOrigin()) {
            throw new IllegalStateException("CORS wildcard is forbidden in prod when credentials are allowed");
        }
        if (isBlank(eventReportSecret) || DEFAULT_EVENT_REPORT_SECRET.equals(eventReportSecret)) {
            throw new IllegalStateException("event report secret must be configured and cannot use the default value");
        }
        if (isBlank(jwtSecret)) {
            throw new IllegalStateException("jwt secret must be configured in prod");
        }
        if ("root".equals(datasourceUsername) || "root".equals(datasourcePassword)) {
            throw new IllegalStateException("root database credentials are forbidden in prod");
        }
        if (secretCipherKey == null || secretCipherKey.isBlank()
                || DEFAULT_SECRET_CIPHER_KEY.equals(secretCipherKey)) {
            throw new IllegalStateException("secret cipher key must be configured and cannot use the default value");
        }
    }

    private boolean containsWildcardOrigin() {
        if (allowedOrigins == null) {
            return false;
        }
        return allowedOrigins.stream()
                .filter(origin -> origin != null)
                .map(String::trim)
                .anyMatch("*"::equals);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
