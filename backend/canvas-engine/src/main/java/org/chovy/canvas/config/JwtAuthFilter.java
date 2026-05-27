package org.chovy.canvas.config;

import org.chovy.canvas.web.AuthController;
import org.chovy.canvas.auth.domain.SysUserService;
import org.chovy.canvas.auth.util.JwtUtil;
import org.chovy.canvas.dal.dataobject.SysUserDO;
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

    /** JWT 工具，用于解析并校验令牌。 */
    private final JwtUtil             jwtUtil;
    /** Redis 模板，用于查询令牌黑名单。 */
    private final StringRedisTemplate redis;
    /** 用户服务，用于按 JWT subject 反查当前用户状态和角色。 */
    private final SysUserService      userService;

    /**
     * 执行 filter 对应的业务逻辑。
     *
     * <p>实现会读写 Redis 中的缓存、锁、路由或运行态数据。
     *
     * @param exchange exchange 方法执行所需的业务参数
     * @param chain chain 方法执行所需的业务参数
     * @return 异步执行结果，订阅后产生节点结果或业务响应
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            // 未携带 Bearer token 时不在这里拦截，交给 Security 的授权规则决定是否允许匿名访问。
            return chain.filter(exchange);
        }

        String token = header.substring(7);
        try {
            // parse 会同时校验签名和过期时间，失败会进入统一 401 分支。
            Claims claims = jwtUtil.parse(token);

            // 检查 JWT 黑名单（服务端登出，设计文档 19.6.1节）
            String hash = AuthController.tokenHash(token);
            return Mono.fromCallable(() -> Boolean.TRUE.equals(redis.hasKey("canvas:jwt:revoked:" + hash)))
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap(revoked -> {
                        if (revoked) {
                            return unauthorized(exchange);
                        }

                        return Mono.fromCallable(() -> loadCurrentUser(claims))
                                .subscribeOn(Schedulers.boundedElastic())
                                .flatMap(user -> {
                                    if (user == null || user.getEnabled() == null || user.getEnabled() != 1
                                            || user.getRole() == null || user.getRole().isBlank()) {
                                        return unauthorized(exchange);
                                    }
                                    // principal 仍保持 Claims 类型，兼容现有控制器；权限以 DB 当前角色为准。
                                    claims.put("username", user.getUsername());
                                    claims.put("role", user.getRole());
                                    claims.put("displayName", user.getDisplayName());
                                    UsernamePasswordAuthenticationToken auth =
                                            new UsernamePasswordAuthenticationToken(
                                                    claims, null,
                                                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
                                            );
                                    return chain.filter(exchange)
                                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
                                });
                    });
        } catch (JwtException | IllegalArgumentException e) {
            return unauthorized(exchange);
        }
    }

    private SysUserDO loadCurrentUser(Claims claims) {
        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            return null;
        }
        return userService.findById(Long.parseLong(subject));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
