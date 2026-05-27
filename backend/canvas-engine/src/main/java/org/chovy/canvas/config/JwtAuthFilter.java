package org.chovy.canvas.config;

import org.chovy.canvas.web.AuthController;
import org.chovy.canvas.auth.util.JwtUtil;
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
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * JWT 认证过滤器。
 *
 * <p>在 WebFlux 请求进入控制器前解析 Authorization 头，校验 token 并把用户身份写入安全上下文。
 * <p>该过滤器是接口鉴权的第一道边界，匿名白名单和异常响应需要在这里保持一致。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements WebFilter {

    private final JwtUtil             jwtUtil;
    private final StringRedisTemplate redis;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        String token = header.substring(7);
        try {
            Claims claims = jwtUtil.parse(token);

            // 检查 JWT 黑名单（服务端登出，设计文档 19.6.1节）
            String hash = AuthController.tokenHash(token);
            return Mono.fromCallable(() -> Boolean.TRUE.equals(redis.hasKey("canvas:jwt:revoked:" + hash)))
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap(revoked -> {
                        if (revoked) {
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        }

                        String role = claims.get("role", String.class);
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                claims, null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );
                        return chain.filter(exchange)
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
                    });
        } catch (JwtException e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }
}
