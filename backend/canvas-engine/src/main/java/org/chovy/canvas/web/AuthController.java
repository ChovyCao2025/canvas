package org.chovy.canvas.web;

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
import org.chovy.canvas.dal.dataobject.SysUserDO;
import org.chovy.canvas.auth.util.JwtUtil;

/**
 * 认证控制器：
 * 负责登录、登出与当前用户信息查询。
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    /** 用户服务，用于校验登录账号与查询当前用户。 */
    private final SysUserService      userService;
    /** Redis 客户端，用于记录登录失败次数和锁定状态。 */
    private final StringRedisTemplate redis;

    /** 登录失败达到锁定的阈值。 */
    private static final int    MAX_FAIL_COUNT  = 5;
    /** 登录锁定时长。 */
    private static final Duration LOCK_TTL      = Duration.ofMinutes(15);
    /** 登录失败计数 Redis Key 前缀。 */
    private static final String FAIL_KEY_PREFIX = "canvas:login:fail:";
    /** 登录锁定 Redis Key 前缀。 */
    private static final String LOCK_KEY_PREFIX = "canvas:login:locked:";
    /** JWT 工具，用于生成和解析登录令牌。 */
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

            // 用户表和密码哈希校验都是阻塞操作，外层 subscribeOn 已切到 boundedElastic。
            SysUserDO user = userService.findByUsernameForAuth(username);
            if (user == null || user.getEnabled() == 0) {
                recordFailedAttempt(username);
                /**
                 * 执行 illegalargumentexception 对应的内部处理流程。
                 * @return 返回内部处理结果
                 */
                throw new IllegalArgumentException("用户名或密码错误");
            }
            if (!userService.checkPassword(user, req.getPassword())) {
                recordFailedAttempt(username);
                /**
                 * 执行 illegalargumentexception 对应的内部处理流程。
                 * @return 返回内部处理结果
                 */
                throw new IllegalArgumentException("用户名或密码错误");
            }

            // 2. 登录成功：清除失败计数
            redis.delete(FAIL_KEY_PREFIX + username);

            // JWT 只写入最小身份 claims，权限细节由后续过滤器转成 Spring Security Authority。
            String token = jwtUtil.generate(user);
            LoginResp resp = new LoginResp();
            resp.setToken(token);
            resp.setUserId(user.getId());
            resp.setTenantId(user.getTenantId());
            resp.setUsername(user.getUsername());
            resp.setDisplayName(user.getDisplayName());
            resp.setRole(user.getRole());
            return R.ok(resp);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /** 记录一次失败尝试；超过阈值则锁定账号 */
    private void recordFailedAttempt(String username) {
        String failKey = FAIL_KEY_PREFIX + username;
        // 失败计数和锁定 key 使用相同 TTL，避免长期保存无效登录噪声。
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
                    // 黑名单 TTL 只保留 token 剩余有效期，过期后 Redis 自动清理。
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
                    // JWT subject 统一存用户 ID，这里反查数据库拿最新展示名和角色。
                    Long userId = Long.parseLong(claims.getSubject());
                    SysUserDO user = userService.findById(userId);
                    if (user == null) throw new IllegalArgumentException("用户不存在");
                    LoginResp resp = new LoginResp();
                    resp.setUserId(user.getId());
                    resp.setTenantId(user.getTenantId());
                    resp.setUsername(user.getUsername());
                    resp.setDisplayName(user.getDisplayName());
                    resp.setRole(user.getRole());
                    return R.ok(resp);
                }).subscribeOn(Schedulers.boundedElastic()));
    }
}
