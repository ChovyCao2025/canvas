package org.chovy.canvas.config;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import org.chovy.canvas.domain.datasource.DataSourceCredentialCipher;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Fails fast when a production-like profile still uses local security defaults.
 */
@Component
public class ProductionSecurityValidator implements SmartInitializingSingleton {

    private static final String LEGACY_EVENT_SECRET = "canvas-event-report-secret-2026!!";
    private static final String LEGACY_JWT_SECRET_PREFIX = "canvas-engine-jwt-secret-key";

    private final Environment environment;

    public ProductionSecurityValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (!isProductionLike(environment)) {
            return;
        }
        List<String> failures = new ArrayList<>();
        validateDatasource(failures);
        validateDataSourceCredentialSecret(failures);
        validateJwt(failures);
        validateEventSecret(failures);
        validateAssetUploadWebhookSecret(failures);
        validateCors(failures);
        validateHealthDetails(failures);
        validateApiDocs(failures);
        if (!failures.isEmpty()) {
            throw new IllegalStateException("生产安全配置不满足要求: " + String.join("; ", failures));
        }
    }

    static boolean isProductionLike(Environment environment) {
        return Arrays.stream(environment.getActiveProfiles())
                .map(profile -> profile.toLowerCase(Locale.ROOT))
                .anyMatch(profile -> profile.equals("prod")
                        || profile.equals("production")
                        || profile.equals("staging")
                        || profile.equals("uat"));
    }

    private void validateDatasource(List<String> failures) {
        String username = value("spring.datasource.username");
        String password = value("spring.datasource.password");
        if ("root".equalsIgnoreCase(username)) {
            failures.add("spring.datasource.username 不能使用 root");
        }
        if ("root".equals(password)) {
            failures.add("spring.datasource.password 不能使用 root");
        }
    }

    private void validateDataSourceCredentialSecret(List<String> failures) {
        String secret = value("canvas.datasource.credential-secret");
        if (secret == null || secret.isBlank()) {
            failures.add("canvas.datasource.credential-secret 必须配置");
            return;
        }
        if (DataSourceCredentialCipher.DEFAULT_SECRET.equals(secret)) {
            failures.add("canvas.datasource.credential-secret 不能使用默认示例密钥");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            failures.add("canvas.datasource.credential-secret 长度不能少于 32 字节");
        }
    }

    private void validateJwt(List<String> failures) {
        String secret = value("canvas.jwt.secret");
        if (secret == null || secret.isBlank()) {
            failures.add("canvas.jwt.secret 必须配置");
            return;
        }
        if (secret.startsWith(LEGACY_JWT_SECRET_PREFIX)) {
            failures.add("canvas.jwt.secret 不能使用默认示例密钥");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            failures.add("canvas.jwt.secret 长度不能少于 32 字节");
        }
    }

    private void validateEventSecret(List<String> failures) {
        String secret = value("canvas.events.report-secret");
        if (secret == null || secret.isBlank()) {
            failures.add("canvas.events.report-secret 必须配置");
            return;
        }
        if (LEGACY_EVENT_SECRET.equals(secret)) {
            failures.add("canvas.events.report-secret 不能使用默认示例密钥");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            failures.add("canvas.events.report-secret 长度不能少于 32 字节");
        }
    }

    private void validateAssetUploadWebhookSecret(List<String> failures) {
        String secret = value("canvas.marketing.content.asset-upload.webhook-secret");
        if (secret == null || secret.isBlank()) {
            failures.add("canvas.marketing.content.asset-upload.webhook-secret 必须配置");
            return;
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            failures.add("canvas.marketing.content.asset-upload.webhook-secret 长度不能少于 32 字节");
        }
    }

    private void validateCors(List<String> failures) {
        String origins = value("canvas.cors.allowed-origins");
        if (origins != null && Arrays.stream(origins.split(","))
                .map(String::trim)
                .anyMatch("*"::equals)) {
            failures.add("canvas.cors.allowed-origins 生产环境不能包含 *");
        }
    }

    private void validateHealthDetails(List<String> failures) {
        String showDetails = value("management.endpoint.health.show-details");
        if ("always".equalsIgnoreCase(showDetails)) {
            failures.add("management.endpoint.health.show-details 生产环境不能为 always");
        }
    }

    private void validateApiDocs(List<String> failures) {
        if ("true".equalsIgnoreCase(value("springdoc.api-docs.enabled"))) {
            failures.add("springdoc.api-docs.enabled 生产环境不能为 true");
        }
        if ("true".equalsIgnoreCase(value("springdoc.swagger-ui.enabled"))) {
            failures.add("springdoc.swagger-ui.enabled 生产环境不能为 true");
        }
    }

    private String value(String key) {
        return environment.getProperty(key);
    }
}
