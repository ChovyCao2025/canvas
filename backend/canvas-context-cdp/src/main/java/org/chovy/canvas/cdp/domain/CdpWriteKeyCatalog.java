package org.chovy.canvas.cdp.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWriteKeyFacade;

/**
 * 维护 CdpWriteKey 的内存目录和查询视图。
 */
public class CdpWriteKeyCatalog {

    /**
     * PREFIX LENGTH。
     */
    private static final int PREFIX_LENGTH = 12;

    private final Map<Long, KeyState> keys = new LinkedHashMap<>();
    /**
     * next Id。
     */
    private long nextId = 1L;

    /**
     * 查询list列表。
     */
    public List<CdpWriteKeyFacade.KeyRow> list(Long tenantId) {
        Long normalizedTenantId = safeTenantId(tenantId);
        return keys.values().stream()
                .filter(key -> normalizedTenantId.equals(key.tenantId))
                .sorted(Comparator.comparing(KeyState::id).reversed())
                .map(CdpWriteKeyCatalog::toRow)
                .toList();
    }

    /**
     * 创建create。
     */
    public CdpWriteKeyFacade.CreateResult create(Long tenantId, CdpWriteKeyFacade.CreateCommand command,
                                                 String actor) {
        CdpWriteKeyFacade.CreateCommand body = command == null
                ? new CdpWriteKeyFacade.CreateCommand(null, null, null, null, null)
                : command;
        Long id = nextId++;
        String rawKey = rawKey(id);
        KeyState state = new KeyState(id, safeTenantId(tenantId), requireText(body.name(), "name"),
                prefix(rawKey), normalizePlatform(body.platform()), "ACTIVE", normalizeQps(body.rateLimitQps()),
                body.dailyQuota(), blankToNull(body.description()), actorOrDefault(actor), timestamp(), timestamp());
        keys.put(id, state);
        return new CdpWriteKeyFacade.CreateResult(state.id, state.name, rawKey, state.keyPrefix, state.platform,
                state.rateLimitQps, state.dailyQuota);
    }

    /**
     * 执行 disable 对应的 CDP 业务操作。
     */
    public void disable(Long tenantId, Long id) {
        KeyState state = keys.get(id);
        if (state == null || !safeTenantId(tenantId).equals(state.tenantId)) {
            throw new IllegalArgumentException("write key is not found");
        }
        state.status = "DISABLED";
        state.updatedAt = timestamp();
    }

    /**
     * 转换为Row。
     */
    private static CdpWriteKeyFacade.KeyRow toRow(KeyState state) {
        return new CdpWriteKeyFacade.KeyRow(state.id, state.name, state.keyPrefix, state.platform, state.status,
                state.rateLimitQps, state.dailyQuota, state.description, state.createdBy, state.createdAt,
                state.updatedAt);
    }

    /**
     * 返回安全的Tenant Id。
     */
    private static Long safeTenantId(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 执行 rawKey 对应的 CDP 业务操作。
     */
    private static String rawKey(Long id) {
        return "ck_live_%048d".formatted(id).replace(' ', '0');
    }

    /**
     * 执行 prefix 对应的 CDP 业务操作。
     */
    private static String prefix(String rawKey) {
        return rawKey.substring(0, Math.min(rawKey.length(), PREFIX_LENGTH));
    }

    /**
     * 读取并校验必填的Text。
     */
    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value.trim();
    }

    /**
     * 归一化Platform。
     */
    private static String normalizePlatform(String platform) {
        if (platform == null || platform.isBlank()) {
            return "WEB";
        }
        return platform.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 归一化Qps。
     */
    private static Integer normalizeQps(Integer rateLimitQps) {
        return rateLimitQps == null || rateLimitQps <= 0 ? 100 : rateLimitQps;
    }

    /**
     * 执行 actorOrDefault 对应的 CDP 业务操作。
     */
    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }

    /**
     * 执行 blankToNull 对应的 CDP 业务操作。
     */
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    /**
     * 执行 timestamp 对应的 CDP 业务操作。
     */
    private static LocalDateTime timestamp() {
        return LocalDateTime.of(2026, 6, 14, 10, 0, 0);
    }

    /**
     * 表示 KeyState 的业务数据或处理组件。
     */
    private static final class KeyState {
        /**
         * 唯一标识。
         */
        private final Long id;

        /**
         * 租户标识。
         */
        private final Long tenantId;

        /**
         * 名称。
         */
        private final String name;

        /**
         * key Prefix。
         */
        private final String keyPrefix;

        /**
         * platform。
         */
        private final String platform;

        /**
         * rate Limit Qps。
         */
        private final Integer rateLimitQps;

        /**
         * daily Quota。
         */
        private final Long dailyQuota;

        /**
         * 描述。
         */
        private final String description;

        /**
         * 创建人。
         */
        private final String createdBy;

        /**
         * 创建时间。
         */
        private final LocalDateTime createdAt;

        /**
         * 状态。
         */
        private String status;

        /**
         * 更新时间。
         */
        private LocalDateTime updatedAt;

        /**
         * 创建当前组件实例。
         */
        private KeyState(Long id, Long tenantId, String name, String keyPrefix, String platform, String status,
                         Integer rateLimitQps, Long dailyQuota, String description, String createdBy,
                         LocalDateTime createdAt, LocalDateTime updatedAt) {
            this.id = id;
            this.tenantId = tenantId;
            this.name = name;
            this.keyPrefix = keyPrefix;
            this.platform = platform;
            this.status = status;
            this.rateLimitQps = rateLimitQps;
            this.dailyQuota = dailyQuota;
            this.description = description;
            this.createdBy = createdBy;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        /**
         * 执行 id 对应的 CDP 业务操作。
         */
        private Long id() {
            return id;
        }
    }
}
