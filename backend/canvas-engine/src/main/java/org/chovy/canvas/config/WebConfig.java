package org.chovy.canvas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class WebConfig {

    /**
     * 允许的跨域来源（生产环境必须配置为实际前端域名）。
     * 开发默认 * 仅供本地，生产通过环境变量覆盖：
     *   CANVAS_CORS_ALLOWED_ORIGINS=https://canvas.yourcompany.com
     */
    @Value("${canvas.cors.allowed-origins:*}")
    private List<String> allowedOrigins;

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        if (allowedOrigins.contains("*")) {
            config.addAllowedOriginPattern("*");
        } else {
            allowedOrigins.forEach(config::addAllowedOrigin);
        }
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
