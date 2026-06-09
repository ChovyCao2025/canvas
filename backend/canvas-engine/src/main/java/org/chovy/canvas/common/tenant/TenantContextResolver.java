package org.chovy.canvas.common.tenant;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * TenantContextResolver 提供 common.tenant 场景的通用基础能力。
 */
@Component
public class TenantContextResolver {

    /**
     * 从响应式安全上下文读取当前租户身份。
     *
     * @return 当前租户上下文；未认证或声明缺失时返回空 Mono
     */
    public Mono<TenantContext> current() {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return ReactiveSecurityContextHolder.getContext()
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .filter(Claims.class::isInstance)
                .cast(Claims.class)
                .map(this::fromClaims);
    }

    /**
     * currentOrError 处理 common.tenant 场景的业务逻辑。
     * @return 返回 currentOrError 流程生成的业务结果。
     */
    public Mono<TenantContext> currentOrError() {
        return current().switchIfEmpty(Mono.error(
                new SecurityException("AUTH_003: missing tenant context")));
    }

    /**
     * 将 JWT Claims 转换为租户上下文。
     *
     * @param claims 已认证用户的 JWT 声明
     * @return 租户上下文
     */
    private TenantContext fromClaims(Claims claims) {
        return new TenantContext(
                readLong(claims.get("tenantId")),
                claims.get("role", String.class),
                claims.get("username", String.class));
    }

    /**
     * 从数字或字符串声明中读取长整型租户编号。
     *
     * @param value JWT 声明原始值
     * @return 可解析的长整型值，无法解析时返回 null
     */
    private Long readLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            String trimmed = text.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            try {
                return Long.valueOf(trimmed);
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
