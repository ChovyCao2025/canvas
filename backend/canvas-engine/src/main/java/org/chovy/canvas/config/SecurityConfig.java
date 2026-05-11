package org.chovy.canvas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

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
                .authorizeExchange(ex -> ex
                        // 公开接口
                        .pathMatchers("/auth/login").permitAll()
                        .pathMatchers("/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs/**", "/webjars/**").permitAll()
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
