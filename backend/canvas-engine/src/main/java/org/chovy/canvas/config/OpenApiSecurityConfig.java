package org.chovy.canvas.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * OpenApiSecurityConfig 提供 config 场景的 Spring 配置或启动校验。
 */
@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
@SecurityScheme(
        name = "triggerHmac",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "X-Canvas-Signature"
)
/**
 * OpenAPI 安全方案配置，声明 JWT Bearer 与触发器 HMAC 请求头认证方式。
 */
public class OpenApiSecurityConfig {
}
