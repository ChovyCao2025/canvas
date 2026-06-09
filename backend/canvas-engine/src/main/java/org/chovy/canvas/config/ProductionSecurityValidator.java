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
 * 生产类环境安全配置校验器，发现本地默认密钥或不安全暴露配置时快速失败。
 */
@Component
public class ProductionSecurityValidator implements SmartInitializingSingleton {

    private static final String LEGACY_EVENT_SECRET = "canvas-event-report-secret-2026!!";
    private static final String LEGACY_JWT_SECRET_PREFIX = "canvas-engine-jwt-secret-key";

    private final Environment environment;

    /**
     * 创建 ProductionSecurityValidator 实例并注入 config 场景依赖。
     * @param environment environment 参数，用于 ProductionSecurityValidator 流程中的校验、计算或对象转换。
     */
    public ProductionSecurityValidator(Environment environment) {
        this.environment = environment;
    }

    /**
     * afterSingletonsInstantiated 处理 config 场景的业务逻辑。
     */
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

    /**
     * 判断当前激活 profile 是否属于生产类环境。
     *
     * @param environment Spring 环境配置
     * @return true 表示 prod、production、staging 或 uat
     */
    static boolean isProductionLike(Environment environment) {
        return Arrays.stream(environment.getActiveProfiles())
                .map(profile -> profile.toLowerCase(Locale.ROOT))
                .anyMatch(profile -> profile.equals("prod")
                        || profile.equals("production")
                        || profile.equals("staging")
                        || profile.equals("uat"));
    }

    /**
     * 校验数据库账号密码未使用 root。
     *
     * @param failures 失败原因收集列表
     */
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

    /**
     * 校验数据源凭据加密密钥已配置且不是默认值。
     *
     * @param failures 失败原因收集列表
     */
    private void validateDataSourceCredentialSecret(List<String> failures) {
        // 准备本次处理所需的上下文和中间变量。
        String secret = value("canvas.datasource.credential-secret");
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (secret == null || secret.isBlank()) {
            failures.add("canvas.datasource.credential-secret 必须配置");
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        if (DataSourceCredentialCipher.DEFAULT_SECRET.equals(secret)) {
            failures.add("canvas.datasource.credential-secret 不能使用默认示例密钥");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            failures.add("canvas.datasource.credential-secret 长度不能少于 32 字节");
        }
    }

    /**
     * 校验 JWT 密钥已配置、足够长且不是示例默认值。
     *
     * @param failures 失败原因收集列表
     */
    private void validateJwt(List<String> failures) {
        // 准备本次处理所需的上下文和中间变量。
        String secret = value("canvas.jwt.secret");
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (secret == null || secret.isBlank()) {
            failures.add("canvas.jwt.secret 必须配置");
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        if (secret.startsWith(LEGACY_JWT_SECRET_PREFIX)) {
            failures.add("canvas.jwt.secret 不能使用默认示例密钥");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            failures.add("canvas.jwt.secret 长度不能少于 32 字节");
        }
    }

    /**
     * 校验事件上报共享密钥已配置、足够长且不是默认值。
     *
     * @param failures 失败原因收集列表
     */
    private void validateEventSecret(List<String> failures) {
        // 准备本次处理所需的上下文和中间变量。
        String secret = value("canvas.events.report-secret");
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (secret == null || secret.isBlank()) {
            failures.add("canvas.events.report-secret 必须配置");
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        if (LEGACY_EVENT_SECRET.equals(secret)) {
            failures.add("canvas.events.report-secret 不能使用默认示例密钥");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            failures.add("canvas.events.report-secret 长度不能少于 32 字节");
        }
    }

    /**
     * 校验营销资产上传回调密钥长度满足生产要求。
     *
     * @param failures 失败原因收集列表
     */
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

    /**
     * 校验生产环境 CORS 来源不包含通配符。
     *
     * @param failures 失败原因收集列表
     */
    private void validateCors(List<String> failures) {
        String origins = value("canvas.cors.allowed-origins");
        if (origins != null && Arrays.stream(origins.split(","))
                .map(String::trim)
                .anyMatch("*"::equals)) {
            failures.add("canvas.cors.allowed-origins 生产环境不能包含 *");
        }
    }

    /**
     * 校验健康检查详情不会在生产环境始终暴露。
     *
     * @param failures 失败原因收集列表
     */
    private void validateHealthDetails(List<String> failures) {
        String showDetails = value("management.endpoint.health.show-details");
        if ("always".equalsIgnoreCase(showDetails)) {
            failures.add("management.endpoint.health.show-details 生产环境不能为 always");
        }
    }

    /**
     * 校验生产环境未开启公开 API 文档。
     *
     * @param failures 失败原因收集列表
     */
    private void validateApiDocs(List<String> failures) {
        if ("true".equalsIgnoreCase(value("springdoc.api-docs.enabled"))) {
            failures.add("springdoc.api-docs.enabled 生产环境不能为 true");
        }
        if ("true".equalsIgnoreCase(value("springdoc.swagger-ui.enabled"))) {
            failures.add("springdoc.swagger-ui.enabled 生产环境不能为 true");
        }
    }

    /**
     * 从环境中读取配置值。
     *
     * @param key 配置键
     * @return 配置值，缺失时为 null
     */
    private String value(String key) {
        return environment.getProperty(key);
    }
}
