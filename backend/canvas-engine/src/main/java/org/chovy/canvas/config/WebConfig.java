package org.chovy.canvas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Web 基础配置：
 * 主要负责跨域策略（CORS）注册。
 *
 * 说明：
 * - 仅处理“浏览器跨域访问”层面问题；
 * - 不处理认证与授权（由 Security 配置负责）。
 */
@Configuration
public class WebConfig {

    private final List<String> allowedOrigins;
    private final boolean productionLike;

    @Autowired
    public WebConfig(
            @Value("${canvas.cors.allowed-origins:*}") List<String> allowedOrigins,
            Environment environment) {
        this(allowedOrigins, ProductionSecurityValidator.isProductionLike(environment));
    }

    WebConfig(List<String> allowedOrigins, boolean productionLike) {
        this.allowedOrigins = allowedOrigins == null || allowedOrigins.isEmpty()
                ? List.of("*")
                : allowedOrigins;
        this.productionLike = productionLike;
    }

    /** 全局 CORS 过滤器。 */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = corsConfiguration();
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 全路径生效，统一在网关/服务层处理跨域策略
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }

    CorsConfiguration corsConfiguration() {
        CorsConfiguration config = new CorsConfiguration();
        if (allowedOrigins.contains("*")) {
            if (productionLike) {
                throw new IllegalStateException("canvas.cors.allowed-origins 生产环境不能包含 *");
            }
            // "*" 通过 allowedOriginPattern 兼容携带凭据场景（本地开发）
            config.addAllowedOriginPattern("*");
        } else {
            allowedOrigins.forEach(config::addAllowedOrigin);
        }
        // 允许全部方法/头，具体接口权限由后端鉴权控制
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        // 允许浏览器在跨域请求中带 cookie / authorization header
        config.setAllowCredentials(true);
        return config;
    }
}
