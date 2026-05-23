package org.chovy.canvas.config;

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

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http, JwtAuthFilter jwtAuthFilter) {

        return http
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
                        .pathMatchers(HttpMethod.POST, "/canvas/execute/direct/**").permitAll()
                        // WebSocket 使用一次性票据鉴权；票据接口本身仍要求登录。
                        .pathMatchers("/canvas/ws/notifications").permitAll()
                        // 运维接口：无需登录（内网调用，不对外暴露）
                        .pathMatchers("/ops/**").permitAll()
                        // 仅 ADMIN 可发布/下线/Kill/灰度/回滚
                        .pathMatchers(HttpMethod.POST,
                                "/canvas/*/publish", "/canvas/*/offline",
                                "/canvas/*/kill", "/canvas/*/canary",
                                "/canvas/*/promote-canary", "/canvas/*/rollback-canary",
                                "/canvas/*/rollback", "/canvas/*/approve", "/canvas/*/reject",
                                "/canvas/import").hasRole("ADMIN")
                        // 管理员接口
                        .pathMatchers("/admin/**").hasRole("ADMIN")
                        // 其余接口需要登录
                        .anyExchange().authenticated()
                )
                .addFilterAt(jwtAuthFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
}
