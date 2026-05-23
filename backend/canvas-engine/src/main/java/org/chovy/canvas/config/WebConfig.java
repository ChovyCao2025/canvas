package org.chovy.canvas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    /**
     * 允许的跨域来源（生产环境必须配置为实际前端域名）。
     * 开发默认 * 仅供本地，生产通过环境变量覆盖：
     *   CANVAS_CORS_ALLOWED_ORIGINS=https://canvas.yourcompany.com
     */
    @Value("${canvas.cors.allowed-origins:*}")
    private List<String> allowedOrigins;

    /** 全局 CORS 过滤器。 */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // "*" 通过 allowedOriginPattern 兼容携带凭据场景（本地开发）
        if (allowedOrigins.contains("*")) {
            config.addAllowedOriginPattern("*");
        } else {
            allowedOrigins.forEach(config::addAllowedOrigin);
        }
        // 允许全部方法/头，具体接口权限由后端鉴权控制
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        // 允许浏览器在跨域请求中带 cookie / authorization header
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 全路径生效，统一在网关/服务层处理跨域策略
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
