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

    @PostMapping("/login")
    public Mono<R<LoginResp>> login(@RequestBody LoginReq req) {
        return Mono.fromCallable(() -> {
            String username = req.getUsername();

            // 1. 检查账号是否已被锁定（设计文档 19.7节）
            if (Boolean.TRUE.equals(redis.hasKey(LOCK_KEY_PREFIX + username))) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        ErrorCode.AUTH_004 + ": 账号已锁定，请 15 分钟后重试");
            }

            SysUser user = userService.findByUsername(username);
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
        redis.expire(failKey, LOCK_TTL);  // 每次失败都重置 TTL（滑动窗口）
        if (count != null && count >= MAX_FAIL_COUNT) {
            redis.opsForValue().set(LOCK_KEY_PREFIX + username, "1", LOCK_TTL);
            log.warn("[AUTH] 账号已锁定 username={} failCount={}", username, count);
        }
    }

    @PostMapping("/logout")
    public Mono<R<Void>> logout() {
        // 前端清 token 即可；如需服务端黑名单，可在此写入 Redis
        return Mono.just(R.ok());
    }

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
