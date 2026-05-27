package org.chovy.canvas.auth.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import org.chovy.canvas.dal.dataobject.SysUserDO;

/**
 * JWT 签发与解析工具。
 *
 * <p>职责边界：
 * 1) 生成 token（写入最小必要 claims）；
 * 2) 校验签名与过期时间并解析 claims。
 */
@Component
public class JwtUtil {

    private static final String LEGACY_DEFAULT_SECRET_PREFIX = "canvas-engine-jwt-secret-key";

    /** HMAC 签名密钥。 */
    private final SecretKey key;

    /** token 过期时长。 */
    private final Duration expiry;

    /** 基于配置初始化签名密钥与过期时间。 */
    public JwtUtil(
            @Value("${canvas.jwt.secret:}") String secret,
            @Value("${canvas.jwt.expiry-hours:24}") long expiryHours) {
        validateSecret(secret);
        if (expiryHours <= 0) {
            throw new IllegalStateException("canvas.jwt.expiry-hours 必须大于 0");
        }
        // HS256 要求至少 256-bit 密钥，启动时 fail-fast，避免默认密钥进入环境。
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiry = Duration.ofHours(expiryHours);
    }

    private static void validateSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("canvas.jwt.secret 未配置，请设置 CANVAS_JWT_SECRET");
        }
        if (secret.startsWith(LEGACY_DEFAULT_SECRET_PREFIX)) {
            throw new IllegalStateException("canvas.jwt.secret 不能使用默认示例密钥");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("canvas.jwt.secret 长度不能少于 32 字节");
        }
    }

    /** 生成访问令牌。 */
    public String generate(SysUserDO user) {
        Date now = new Date();
        return Jwts.builder()
                // sub 统一存用户 ID，便于后续扩展通用身份解析
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .claim("role", user.getRole())
                .claim("displayName", user.getDisplayName())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiry.toMillis()))
                .signWith(key)
                .compact();
    }

    /** 解析并校验令牌。 */
    public Claims parse(String token) {
        // parseSignedClaims 会同时做签名校验与过期校验，失败抛 JwtException
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
