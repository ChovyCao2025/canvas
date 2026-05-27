package org.chovy.canvas.auth.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
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

    /** HMAC 签名密钥。 */
    private final SecretKey key;

    /** token 过期时长。 */
    private final Duration expiry;

    /** 基于配置初始化签名密钥与过期时间。 */
    public JwtUtil(
            @Value("${canvas.jwt.secret:canvas-engine-jwt-secret-key-must-be-at-least-256-bits}") String secret,
            @Value("${canvas.jwt.expiry-hours:24}") long expiryHours) {
        // HS256 要求足够长度的密钥；默认值仅用于本地开发。
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expiry = Duration.ofHours(expiryHours);
    }

    /** 生成访问令牌。 */
    public String generate(SysUserDO user) {
        Date now = new Date();
        return Jwts.builder()
                // sub 统一存用户 ID，便于后续扩展通用身份解析
                .subject(String.valueOf(user.getId()))
                .claim("tenantId", user.getTenantId())
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
