package org.chovy.canvas.config;

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
import reactor.core.publisher.Mono;

/**
 * WebFlux 安全配置：
 * 定义认证入口、接口权限策略以及 JWT 过滤器挂载顺序。
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    static final String[] SUPER_ADMIN_ROUTE_ROLES = {RoleNames.ADMIN, RoleNames.SUPER_ADMIN};
    static final String[] TENANT_ADMIN_ROUTE_ROLES = {
            RoleNames.ADMIN,
            RoleNames.SUPER_ADMIN,
            RoleNames.TENANT_ADMIN
    };
    /** 密码编码器（BCrypt）。 */
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** 主安全过滤链。 */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http, JwtAuthFilter jwtAuthFilter) {

        return http
                // API 服务不依赖浏览器表单态，关闭有状态防护入口后统一走 JWT。
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                // 未登录时返回 JSON 401，不触发浏览器原生 Basic Auth 弹窗
                .exceptionHandling(ex -> ex.authenticationEntryPoint((exchange, e) -> {
                    var response = exchange.getResponse();
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    var body = "{\"code\":-1,\"message\":\"未登录或 Token 已过期\",\"data\":null}";
                    var buffer = response.bufferFactory().wrap(body.getBytes());
                    return response.writeWith(Mono.just(buffer));
                }))
                .authorizeExchange(ex -> ex
                        // 公开接口
                        .pathMatchers("/auth/login").permitAll()
                        .pathMatchers("/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs/**", "/webjars/**").permitAll()
                        // OpenAPI：事件上报无需登录（业务系统直接调用）
                        .pathMatchers(HttpMethod.POST, "/canvas/events/report").permitAll()
                        // OpenAPI：直调执行无需登录，但控制器必须校验 HMAC 签名。
                        .pathMatchers(HttpMethod.POST, "/canvas/execute/direct/*").permitAll()
                        // OpenAPI：行为触发无需登录，但控制器必须校验 HMAC 签名。
                        .pathMatchers(HttpMethod.POST, "/canvas/trigger/behavior").permitAll()
                        // WebSocket 使用一次性票据鉴权；票据接口本身仍要求登录。
                        .pathMatchers("/canvas/ws/notifications").permitAll()
                        // 运维接口：必须由管理员调用。
                        .pathMatchers("/ops/**").hasAnyRole(SUPER_ADMIN_ROUTE_ROLES)
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
                        // 租户管理：rollout 期间 legacy ADMIN 与新 SUPER_ADMIN 都视为超级管理员。
                        .pathMatchers("/admin/tenants", "/admin/tenants/**")
                        .hasAnyRole(SUPER_ADMIN_ROUTE_ROLES)
                        .pathMatchers("/admin/users", "/admin/users/**")
                        .hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        // 管理员接口
                        .pathMatchers("/admin/**").hasAnyRole(TENANT_ADMIN_ROUTE_ROLES)
                        // 其余接口需要登录
                        .anyExchange().authenticated()
                )
                // JWT 过滤器必须位于认证阶段，先解析身份再进入后续授权判断。
                .addFilterAt(jwtAuthFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
}
