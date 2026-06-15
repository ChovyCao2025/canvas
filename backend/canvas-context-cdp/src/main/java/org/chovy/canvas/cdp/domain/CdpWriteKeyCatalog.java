package org.chovy.canvas.cdp.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWriteKeyFacade;

public class CdpWriteKeyCatalog {

    private static final int PREFIX_LENGTH = 12;

    private final Map<Long, KeyState> keys = new LinkedHashMap<>();
    private long nextId = 1L;

    public List<CdpWriteKeyFacade.KeyRow> list(Long tenantId) {
        Long normalizedTenantId = safeTenantId(tenantId);
        return keys.values().stream()
                .filter(key -> normalizedTenantId.equals(key.tenantId))
                .sorted(Comparator.comparing(KeyState::id).reversed())
                .map(CdpWriteKeyCatalog::toRow)
                .toList();
    }

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

    public void disable(Long tenantId, Long id) {
        KeyState state = keys.get(id);
        if (state == null || !safeTenantId(tenantId).equals(state.tenantId)) {
            throw new IllegalArgumentException("write key is not found");
        }
        state.status = "DISABLED";
        state.updatedAt = timestamp();
    }

    private static CdpWriteKeyFacade.KeyRow toRow(KeyState state) {
        return new CdpWriteKeyFacade.KeyRow(state.id, state.name, state.keyPrefix, state.platform, state.status,
                state.rateLimitQps, state.dailyQuota, state.description, state.createdBy, state.createdAt,
                state.updatedAt);
    }

    private static Long safeTenantId(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private static String rawKey(Long id) {
        return "ck_live_%048d".formatted(id).replace(' ', '0');
    }

    private static String prefix(String rawKey) {
        return rawKey.substring(0, Math.min(rawKey.length(), PREFIX_LENGTH));
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value.trim();
    }

    private static String normalizePlatform(String platform) {
        if (platform == null || platform.isBlank()) {
            return "WEB";
        }
        return platform.trim().toUpperCase(Locale.ROOT);
    }

    private static Integer normalizeQps(Integer rateLimitQps) {
        return rateLimitQps == null || rateLimitQps <= 0 ? 100 : rateLimitQps;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static LocalDateTime timestamp() {
        return LocalDateTime.of(2026, 6, 14, 10, 0, 0);
    }

    private static final class KeyState {
        private final Long id;
        private final Long tenantId;
        private final String name;
        private final String keyPrefix;
        private final String platform;
        private final Integer rateLimitQps;
        private final Long dailyQuota;
        private final String description;
        private final String createdBy;
        private final LocalDateTime createdAt;
        private String status;
        private LocalDateTime updatedAt;

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

        private Long id() {
            return id;
        }
    }
}
