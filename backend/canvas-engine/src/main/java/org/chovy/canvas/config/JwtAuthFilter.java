package org.chovy.canvas.config;

import org.chovy.canvas.auth.controller.AuthController;
import org.chovy.canvas.auth.domain.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * WebFlux JWT 认证过滤器。
 *
 * <p>该过滤器只负责“鉴权与注入身份”，不做业务授权。
 * 角色授权由 Spring Security 路由规则与注解完成。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements WebFilter {

    /** JWT 签发/解析工具。 */
    private final JwtUtil             jwtUtil;

    /** Redis（用于 token 黑名单校验）。 */
    private final StringRedisTemplate redis;

    /**
     * JWT 过滤流程：
     * 1) 解析 Bearer token
     * 2) 校验签名/过期
     * 3) 校验黑名单（登出场景）
     * 4) 写入 SecurityContext
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // 无 Authorization 头时直接放行，由后续鉴权规则决定是否允许匿名访问
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        String token = header.substring(7);
        try {
            Claims claims = jwtUtil.parse(token);

            // 检查 JWT 黑名单（服务端登出，设计文档 19.6.1节）
            String hash = AuthController.tokenHash(token);
            if (Boolean.TRUE.equals(redis.hasKey("canvas:jwt:revoked:" + hash))) {
                // 命中黑名单立即返回 401，避免使用已注销 token 继续访问
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String role = claims.get("role", String.class);
            // 约定角色写成 ROLE_xxx，匹配 Spring Security 默认前缀策略
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    claims, null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
            );
            // 把认证态写入 Reactive Security Context，后续授权规则可直接读取
            return chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
        } catch (JwtException e) {
            // token 非法或过期统一按未认证处理
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }
}
