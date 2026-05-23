package org.chovy.canvas.auth.controller;

import org.chovy.canvas.auth.domain.*;
import org.chovy.canvas.auth.dto.*;
import org.chovy.canvas.common.ErrorCode;
import org.chovy.canvas.common.R;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

/**
 * 认证控制器：
 * 负责登录、登出与当前用户信息查询。
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService      userService;
    private final StringRedisTemplate redis;

    private static final int    MAX_FAIL_COUNT  = 5;
    private static final Duration LOCK_TTL      = Duration.ofMinutes(15);
    private static final String FAIL_KEY_PREFIX = "canvas:login:fail:";
    private static final String LOCK_KEY_PREFIX = "canvas:login:locked:";
    private final JwtUtil jwtUtil;

    /**
     * 登录：
     * 校验锁定状态 -> 用户凭证校验 -> 生成 JWT -> 返回登录信息。
     */
    @PostMapping("/login")
    public Mono<R<LoginResp>> login(@RequestBody LoginReq req) {
        return Mono.fromCallable(() -> {
            String username = req.getUsername();

            // 1. 检查账号是否已被锁定（设计文档 19.7节）
            if (Boolean.TRUE.equals(redis.hasKey(LOCK_KEY_PREFIX + username))) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        ErrorCode.AUTH_004 + ": 账号已锁定，请 15 分钟后重试");
            }

            SysUser user = userService.findByUsernameForAuth(username);
            if (user == null || user.getEnabled() == 0) {
                recordFailedAttempt(username);
                throw new IllegalArgumentException("用户名或密码错误");
            }
            if (!userService.checkPassword(user, req.getPassword())) {
                recordFailedAttempt(username);
                throw new IllegalArgumentException("用户名或密码错误");
            }

            // 2. 登录成功：清除失败计数
            redis.delete(FAIL_KEY_PREFIX + username);

            String token = jwtUtil.generate(user);
            LoginResp resp = new LoginResp();
            resp.setToken(token);
            resp.setUserId(user.getId());
            resp.setUsername(user.getUsername());
            resp.setDisplayName(user.getDisplayName());
            resp.setRole(user.getRole());
            return R.ok(resp);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** 记录一次失败尝试；超过阈值则锁定账号 */
    private void recordFailedAttempt(String username) {
        String failKey = FAIL_KEY_PREFIX + username;
        Long count = redis.opsForValue().increment(failKey);
        redis.expire(failKey, LOCK_TTL);
        if (count != null && count >= MAX_FAIL_COUNT) {
            redis.opsForValue().set(LOCK_KEY_PREFIX + username, "1", LOCK_TTL);
            log.warn("[AUTH] 账号已锁定 username={} failCount={}", username, count);
        }
    }

    /**
     * 登出：将当前 token 加入服务端黑名单（设计文档 19.6.1节）。
     * token 在剩余有效期内不再被接受，即使 JWT 签名合法。
     */
    @PostMapping("/logout")
    public Mono<R<Void>> logout(org.springframework.web.server.ServerWebExchange exchange) {
        return Mono.fromRunnable(() -> {
            String header = exchange.getRequest().getHeaders()
                    .getFirst(org.springframework.http.HttpHeaders.AUTHORIZATION);
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                try {
                    io.jsonwebtoken.Claims claims = jwtUtil.parse(token);
                    long remaining = claims.getExpiration().getTime() - System.currentTimeMillis();
                    if (remaining > 0) {
                        // key = 前32位 SHA-256 hash，固定长度，不泄露 token 内容
                        String hash = tokenHash(token);
                        redis.opsForValue().set(
                            "canvas:jwt:revoked:" + hash, "1",
                            java.time.Duration.ofMillis(remaining));
                        log.debug("[AUTH] token 已加入黑名单 hash={}", hash);
                    }
                } catch (Exception ignored) {
                    // token 已过期或格式错误，无需加入黑名单
                }
            }
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
          .thenReturn(R.<Void>ok());
    }

    /** 计算 token SHA-256 的前32个十六进制字符 */
    public static String tokenHash(String token) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest, 0, 16);
        } catch (Exception e) {
            return String.valueOf(token.hashCode());
        }
    }

    /** 获取当前登录用户信息（从 JWT claims 反查用户）。 */
    @GetMapping("/me")
    public Mono<R<LoginResp>> me() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Authentication::getPrincipal)
                .cast(Claims.class)
                .flatMap(claims -> Mono.fromCallable(() -> {
                    Long userId = Long.parseLong(claims.getSubject());
                    SysUser user = userService.findById(userId);
                    if (user == null) throw new IllegalArgumentException("用户不存在");
                    LoginResp resp = new LoginResp();
                    resp.setUserId(user.getId());
                    resp.setUsername(user.getUsername());
                    resp.setDisplayName(user.getDisplayName());
                    resp.setRole(user.getRole());
                    return R.ok(resp);
                }).subscribeOn(Schedulers.boundedElastic()));
    }
}
