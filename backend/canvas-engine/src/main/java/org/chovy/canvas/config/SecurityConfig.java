package org.chovy.canvas.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.ErrorCode;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.RoleNames;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.core.env.Environment;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

/**
 * WebFlux 安全配置：
 * 定义认证入口、接口权限策略以及 JWT 过滤器挂载顺序。
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static final String[] SUPER_ADMIN_ROUTE_ROLES = {RoleNames.ADMIN, RoleNames.SUPER_ADMIN};
    static final String[] OPS_READ_ROUTE_ROLES = {
            RoleNames.ADMIN,
            RoleNames.SUPER_ADMIN,
            RoleNames.TENANT_ADMIN,
            RoleNames.OPERATOR
    };
    static final String[] OPS_WRITE_ROUTE_ROLES = {
            RoleNames.ADMIN,
            RoleNames.SUPER_ADMIN,
            RoleNames.TENANT_ADMIN
    };
    static final String[] OPS_ROUTE_ROLES = OPS_WRITE_ROUTE_ROLES;
    static final String[] TENANT_ADMIN_ROUTE_ROLES = {
            RoleNames.ADMIN,
            RoleNames.SUPER_ADMIN,
            RoleNames.TENANT_ADMIN
    };
    static final String[] CONTENT_EDITOR_ROUTE_ROLES = {
            RoleNames.ADMIN,
            RoleNames.SUPER_ADMIN,
            RoleNames.TENANT_ADMIN,
            RoleNames.OPERATOR
    };
    static final String[] CONTENT_PUBLISH_ROUTE_ROLES = TENANT_ADMIN_ROUTE_ROLES;
    static final String[] INTERNAL_OPEN_API_ROUTES = {
            "/canvas/events/report",
            "/canvas/execute/direct/*",
            "/canvas/trigger/behavior",
            "/warehouse/realtime/pipelines/checkpoints"
    };
    static final String[] DOCUMENTATION_ROUTES = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/webjars/**"
    };
    /** 密码编码器（BCrypt）。 */
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** 主安全过滤链。 */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            JwtAuthFilter jwtAuthFilter,
            InternalApiAuthFilter internalApiAuthFilter,
            Environment environment) {

        return http
                // API 服务不依赖浏览器表单态，关闭有状态防护入口后统一走 JWT。
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                // 未登录时返回 JSON 401，不触发浏览器原生 Basic Auth 弹窗
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((exchange, e) -> writeError(exchange, HttpStatus.UNAUTHORIZED,
                                ErrorCode.AUTH_002, "未登录或 Token 已过期"))
                        .accessDeniedHandler((exchange, e) -> writeError(exchange, HttpStatus.FORBIDDEN,
                                ErrorCode.AUTH_003, "无权限执行此操作")))
                .authorizeExchange(ex -> {
                    ServerHttpSecurity.AuthorizeExchangeSpec exchanges = ex
                        // 公开接口
                        .pathMatchers("/auth/login").permitAll()
                        .pathMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        ;
                    if (publicDocumentationEnabled(environment)) {
                        exchanges = exchanges.pathMatchers(DOCUMENTATION_ROUTES).permitAll();
                    }
                    exchanges
                        // OpenAPI：事件上报无需登录（业务系统直接调用）
                        .pathMatchers(HttpMethod.POST, INTERNAL_OPEN_API_ROUTES).permitAll()
                        // CDP SDK ingestion is anonymous at the filter layer and validates write keys in controller.
                        .pathMatchers(HttpMethod.POST, "/cdp/events/track").permitAll()
                        // OpenAPI：直调执行入口。
                        .pathMatchers(HttpMethod.POST, "/canvas/execute/direct/*").permitAll()
                        // OpenAPI：行为触发入口。
                        .pathMatchers(HttpMethod.POST, "/canvas/trigger/behavior").permitAll()
                        // WebSocket 使用一次性票据鉴权；票据接口本身仍要求登录。
                        .pathMatchers("/canvas/ws/notifications").permitAll()
                        // Provider receipt callback uses its own shared-secret validation in the controller.
                        .pathMatchers(HttpMethod.POST, "/delivery/receipts").permitAll()
                        // BI embed render verifies a signed short-lived ticket without exposing ticket creation.
                        .pathMatchers(HttpMethod.POST, "/canvas/bi/embed-tickets/verify").permitAll()
                        .pathMatchers(HttpMethod.POST, "/canvas/bi/embed/query/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/canvas/bi/embed/resources/dashboard").permitAll()
                        .pathMatchers(HttpMethod.POST, "/canvas/bi/embed/resources/dashboard/runtime-state").permitAll()
                        .pathMatchers(HttpMethod.POST, "/canvas/bi/embed/resources/portal").permitAll()
                        // Public marketing forms are anonymous lead-capture endpoints.
                        .pathMatchers(HttpMethod.GET, "/public/marketing-forms/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/public/marketing-forms/**").permitAll()
                        // Public marketing monitoring webhooks are anonymous at the filter layer and validate HMAC signatures in controller.
                        .pathMatchers(HttpMethod.POST, "/public/marketing-monitoring/webhooks/**").permitAll()
                        // Public asset provider callbacks are anonymous at the filter layer and validate HMAC signatures in controller.
                        .pathMatchers(HttpMethod.POST, "/public/marketing/content/assets/upload-callbacks/**").permitAll()
                        // Provider conversation webhooks are anonymous at the filter layer and validate signatures in controller.
                        .pathMatchers(HttpMethod.GET, "/public/conversation-webhooks/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/public/conversation-webhooks/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/public/conversations/webhooks/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/public/conversations/webhooks/**").permitAll()
                        // 运维接口：读状态允许运营角色，写控制面只允许租户管理员以上。
                        .pathMatchers(HttpMethod.GET, "/ops/**").hasAnyRole(OPS_READ_ROUTE_ROLES)
                        .pathMatchers("/ops/**").hasAnyRole(OPS_WRITE_ROUTE_ROLES)
                        // 投递 outbox 查询、重放和 reconcile 是运营控制面，只允许租户管理员以上角色。
                        .pathMatchers("/message-deliveries", "/message-deliveries/**")
                        .hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        // 内容营销：草稿和上传意图允许营销运营，审批、发布、回滚和手工 READY 变更只允许租户管理员以上。
                        .pathMatchers(HttpMethod.POST,
                                "/marketing/content/asset-folders",
                                "/marketing/content/assets/upload-intents",
                                "/marketing/content/templates",
                                "/marketing/content/entries",
                                "/message-templates")
                        .hasAnyRole(CONTENT_EDITOR_ROUTE_ROLES)
                        .pathMatchers(HttpMethod.POST,
                                "/marketing/content/assets",
                                "/marketing/content/assets/*/status",
                                "/marketing/content/assets/upload-intents/expire-stale",
                                "/marketing/content/templates/*/status",
                                "/marketing/content/entries/*/publish",
                                "/marketing/content/entries/*/archive",
                                "/marketing/content/releases/publish",
                                "/marketing/content/releases/*/rollback")
                        .hasAnyRole(CONTENT_PUBLISH_ROUTE_ROLES)
                        // 画布管理动作：SaaS rollout 期间允许 legacy ADMIN、新 SUPER_ADMIN、TENANT_ADMIN。
                        .pathMatchers(HttpMethod.POST,
                                "/canvas/*/publish", "/canvas/*/offline",
                                "/canvas/*/kill", "/canvas/*/canary",
                                "/canvas/*/promote-canary", "/canvas/*/rollback-canary",
                                "/canvas/*/rollback", "/canvas/*/approve", "/canvas/*/reject",
                                "/canvas/*/archive", "/canvas/*/revert/*", "/canvas/*/clone",
                                "/canvas/*/save-as-template", "/canvas/from-template/*",
                                "/canvas/import").hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        .pathMatchers(HttpMethod.PUT, "/canvas/*", "/canvas/*/safe")
                        .hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        .pathMatchers("/canvas/data-sources/**")
                        .hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        .pathMatchers("/canvas/api-definitions/**")
                        .hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        .pathMatchers("/canvas/tag-import-sources/**")
                        .hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        // High-impact tenant control-plane writes require tenant-admin level access.
                        .pathMatchers(HttpMethod.POST,
                                "/canvas/bi/permissions/resources",
                                "/canvas/bi/permissions/rows",
                                "/canvas/bi/permissions/columns",
                                "/canvas/bi/permissions/requests/*/review")
                        .hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        .pathMatchers(HttpMethod.DELETE,
                                "/canvas/bi/permissions/resources/*",
                                "/canvas/bi/permissions/rows/*",
                                "/canvas/bi/permissions/columns/*")
                        .hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        .pathMatchers(HttpMethod.POST, "/cdp/write-keys")
                        .hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        .pathMatchers(HttpMethod.DELETE, "/cdp/write-keys/*")
                        .hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        .pathMatchers(HttpMethod.POST, "/cdp/webhooks/**")
                        .hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        .pathMatchers(HttpMethod.PUT, "/cdp/webhooks/**")
                        .hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        .pathMatchers(HttpMethod.DELETE, "/cdp/webhooks/**")
                        .hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        .pathMatchers(HttpMethod.POST, "/warehouse/**")
                        .hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        .pathMatchers(HttpMethod.POST,
                                "/canvas/execution-requests/*/replay",
                                "/canvas/execution-requests/replay")
                        .hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        .pathMatchers(HttpMethod.POST, "/ai/providers")
                        .hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        .pathMatchers(HttpMethod.PUT, "/ai/providers/*")
                        .hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        .pathMatchers(HttpMethod.POST, "/ai/providers/*/disable")
                        .hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        .pathMatchers(HttpMethod.POST,
                                "/channels/connectors/*/mode",
                                "/channels/connectors/*/health-test",
                                "/channels/connectors/fallback/validate")
                        .hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        .pathMatchers(HttpMethod.POST,
                                "/canvas/search-marketing/mutations/*/approve",
                                "/canvas/search-marketing/mutations/*/execute",
                                "/canvas/search-marketing/mutations/*/reconcile",
                                "/canvas/search-marketing/sources/*/sync",
                                "/canvas/search-marketing/sources/sync-due")
                        .hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        .pathMatchers(HttpMethod.POST,
                                "/canvas/creator-collaboration/mutations/*/approve",
                                "/canvas/creator-collaboration/mutations/*/execute")
                        .hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        .pathMatchers(HttpMethod.POST,
                                "/canvas/programmatic-dsp/mutations/*/approve",
                                "/canvas/programmatic-dsp/mutations/*/execute")
                        .hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        .pathMatchers(HttpMethod.POST,
                                "/canvas/marketing-monitoring/provider-credentials/**",
                                "/canvas/marketing-monitoring/sources/*/webhook-secret/rotate",
                                "/canvas/marketing-monitoring/sources/*/polling",
                                "/canvas/marketing-monitoring/sources/*/poll")
                        .hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        .pathMatchers(HttpMethod.POST,
                                "/canvas/paid-media/audience-sync/destinations",
                                "/canvas/paid-media/audience-sync/runs")
                        .hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        .pathMatchers(HttpMethod.POST,
                                "/canvas/loyalty/users/*/earn",
                                "/canvas/loyalty/users/*/redeem")
                        .hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        .pathMatchers(HttpMethod.POST,
                                "/canvas/growth-activities",
                                "/canvas/growth-activities/**")
                        .hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        .pathMatchers(HttpMethod.POST,
                                "/canvas/ab-experiments",
                                "/canvas/ab-experiments/**")
                        .hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        .pathMatchers(HttpMethod.PUT, "/canvas/ab-experiments/**")
                        .hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        .pathMatchers(HttpMethod.DELETE, "/canvas/ab-experiments/**")
                        .hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        .pathMatchers(HttpMethod.POST,
                                "/canvas/policies/consent",
                                "/canvas/policies/suppression",
                                "/canvas/policies/channel")
                        .hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        .pathMatchers(HttpMethod.POST, "/canvas/marketing-integrations/**")
                        .hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        .pathMatchers(HttpMethod.DELETE, "/canvas/marketing-integrations/**")
                        .hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        // 租户管理：rollout 期间 legacy ADMIN 与新 SUPER_ADMIN 都视为超级管理员。
                        .pathMatchers("/admin/tenants", "/admin/tenants/**")
                        .hasAnyRole(SUPER_ADMIN_ROUTE_ROLES)
                        .pathMatchers("/admin/users", "/admin/users/**")
                        .hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        // 管理员接口
                        .pathMatchers("/admin/**").hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        // 其余接口需要登录
                        .anyExchange().authenticated();
                })
                // JWT 过滤器必须位于认证阶段，先解析身份再进入后续授权判断。
                .addFilterAt(internalApiAuthFilter, SecurityWebFiltersOrder.FIRST)
                .addFilterAt(jwtAuthFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    /**
     * 写入统一 JSON 安全错误响应。
     *
     * @param exchange 当前请求交换对象
     * @param status HTTP 状态码
     * @param errorCode 业务错误码
     * @param message 错误消息
     * @return 响应写入完成信号
     */
    private static Mono<Void> writeError(
            org.springframework.web.server.ServerWebExchange exchange,
            HttpStatus status,
            String errorCode,
            String message) {
        var response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        R<Void> body = R.fail(errorCode, status.value(), message, currentTraceId());
        var buffer = response.bufferFactory().wrap(toJson(body));
        return response.writeWith(Mono.just(buffer));
    }

    /**
     * 获取当前 traceId，缺失时生成新的 UUID。
     *
     * @return traceId
     */
    private static String currentTraceId() {
        return CorrelationIdWebFilter.currentTraceId()
                .orElseGet(() -> UUID.randomUUID().toString());
    }

    /**
     * 将统一响应体序列化为 JSON 字节，序列化失败时返回手工兜底 JSON。
     *
     * @param body 统一响应体
     * @return JSON 字节数组
     */
    private static byte[] toJson(R<Void> body) {
        try {
            return OBJECT_MAPPER.writeValueAsBytes(body);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (JsonProcessingException e) {
            String fallback = String.format(
                    "{\"code\":%d,\"errorCode\":\"%s\",\"message\":\"%s\",\"data\":null,\"traceId\":\"%s\"}",
                    body.getCode(),
                    body.getErrorCode(),
                    body.getMessage(),
                    body.getTraceId());
            return fallback.getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * 判断当前环境是否允许公开 Swagger/OpenAPI 文档。
     *
     * @param environment Spring 环境配置
     * @return true 表示非生产类环境允许访问文档
     */
    static boolean publicDocumentationEnabled(Environment environment) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (environment == null) {
            return true;
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return Arrays.stream(environment.getActiveProfiles())
                .map(profile -> profile == null ? "" : profile.trim().toLowerCase(java.util.Locale.ROOT))
                .noneMatch(profile -> profile.equals("prod") || profile.equals("production") || profile.equals("staging"));
    }
}
