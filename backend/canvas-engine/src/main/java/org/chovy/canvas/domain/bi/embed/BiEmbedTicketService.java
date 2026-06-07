package org.chovy.canvas.domain.bi.embed;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.chovy.canvas.dal.dataobject.BiAuditLogDO;
import org.chovy.canvas.dal.dataobject.BiEmbedTokenDO;
import org.chovy.canvas.dal.mapper.BiAuditLogMapper;
import org.chovy.canvas.dal.mapper.BiEmbedTokenMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

@Component
public class BiEmbedTicketService {

    private static final int MIN_TTL_SECONDS = 60;
    private static final int DEFAULT_TTL_SECONDS = 600;
    private static final int MAX_TTL_SECONDS = 1800;
    private static final int MAX_FILTERS = 16;
    private static final int MAX_PARAMETERS = 32;
    private static final int MAX_ALLOWED_DOMAINS = 8;
    private static final int DEFAULT_MAX_ACCESS_COUNT = 1;
    private static final int MAX_ACCESS_COUNT = 100;
    private static final int DEFAULT_RATE_LIMIT_PER_MINUTE = 60;
    private static final int MAX_RATE_LIMIT_PER_MINUTE = 600;
    private static final Pattern SAFE_KEY = Pattern.compile("[A-Za-z0-9_-]{1,80}");
    private static final Pattern SAFE_DOMAIN = Pattern.compile("[a-z0-9][a-z0-9.-]{0,252}(:[0-9]{1,5})?");
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SCOPE_EXTERNAL_TICKET = "EXTERNAL_TICKET";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    private final String secret;
    private final Clock clock;
    private final BiEmbedTokenMapper tokenMapper;
    private final BiAuditLogMapper auditLogMapper;
    private final ConcurrentMap<String, InMemoryAccessState> consumedNonces = new ConcurrentHashMap<>();

    @Autowired
    public BiEmbedTicketService(@Value("${canvas.bi.embed-secret:${canvas.jwt.secret:}}") String secret,
                                ObjectProvider<BiEmbedTokenMapper> tokenMapperProvider,
                                ObjectProvider<BiAuditLogMapper> auditLogMapperProvider) {
        this(secret,
                Clock.systemUTC(),
                tokenMapperProvider == null ? null : tokenMapperProvider.getIfAvailable(),
                auditLogMapperProvider == null ? null : auditLogMapperProvider.getIfAvailable());
    }

    public BiEmbedTicketService(String secret, Clock clock) {
        this(secret, clock, null, null);
    }

    public BiEmbedTicketService(String secret,
                                Clock clock,
                                BiEmbedTokenMapper tokenMapper,
                                BiAuditLogMapper auditLogMapper) {
        validateSecret(secret);
        this.secret = secret;
        this.clock = clock;
        this.tokenMapper = tokenMapper;
        this.auditLogMapper = auditLogMapper;
    }

    public static BiEmbedTicketService testService() {
        return new BiEmbedTicketService("bi-embed-test-secret-with-at-least-32-bytes", Clock.systemUTC());
    }

    public BiEmbedTicket createTicket(Long tenantId, String username, BiEmbedTicketRequest request) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        String safeUser = requireText(username, "username");
        String resourceType = requireSafeKey(request == null ? null : request.resourceType(), "resourceType");
        String resourceKey = requireSafeKey(request.resourceKey(), "resourceKey");
        String scope = requireSafeKey(request.scope(), "scope");
        Map<String, String> filters = sanitizeFilters(request.filters());
        Map<String, String> parameters = sanitizeParameters(request.parameters());
        List<String> allowedDomains = sanitizeAllowedDomains(request.allowedDomains());
        int maxAccessCount = maxAccessCount(request.maxAccessCount());
        int rateLimitPerMinute = rateLimitPerMinute(request.rateLimitPerMinute());
        if (SCOPE_EXTERNAL_TICKET.equals(scope) && allowedDomains.isEmpty()) {
            throw new IllegalArgumentException("allowedDomains are required for external BI embed tickets");
        }
        Instant issuedAt = Instant.now(clock);
        Instant expiresAt = issuedAt.plusSeconds(ttl(request.ttlSeconds()));
        BiEmbedTicketPayload payload = new BiEmbedTicketPayload(
                tenantId,
                safeUser,
                resourceType,
                resourceKey,
                scope,
                filters,
                parameters,
                allowedDomains,
                maxAccessCount,
                rateLimitPerMinute,
                UUID.randomUUID().toString().replace("-", ""),
                issuedAt,
                expiresAt
        );
        String payloadPart = base64Url(toJson(payload));
        String signature = sign(payloadPart);
        String ticket = payloadPart + "." + signature;
        persistToken(ticket, payload);
        return new BiEmbedTicket(ticket, expiresAt,
                "/bi/embed/" + resourceType + "/" + resourceKey + "?ticket=" + ticket);
    }

    public BiEmbedTicketPayload verify(String ticket) {
        if (ticket == null || ticket.isBlank() || !ticket.contains(".")) {
            throw new SecurityException("invalid BI embed ticket");
        }
        String[] parts = ticket.split("\\.", 2);
        String expected = sign(parts[0]);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), parts[1].getBytes(StandardCharsets.UTF_8))) {
            throw new SecurityException("invalid BI embed ticket");
        }
        BiEmbedTicketPayload payload = fromJson(new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8));
        if (!payload.expiresAt().isAfter(Instant.now(clock))) {
            throw new SecurityException("expired BI embed ticket");
        }
        return payload;
    }

    public BiEmbedTicketPayload verifyForUse(String ticket, String origin) {
        BiEmbedTicketPayload payload = verify(ticket);
        String normalizedOrigin = enforceAllowedOrigin(payload, origin);
        if (tokenMapper != null) {
            consumePersistentToken(ticket, payload, normalizedOrigin);
            return payload;
        }
        consumeInMemory(payload);
        return payload;
    }

    public BiEmbedTokenCleanupResult cleanupExpiredTokens(Long tenantId, int limit) {
        purgeExpiredInMemoryNonces();
        if (tokenMapper == null) {
            return new BiEmbedTokenCleanupResult(0, 0, 0);
        }
        Long scopedTenantId = tenantId == null ? 0L : tenantId;
        int capped = Math.max(1, Math.min(limit <= 0 ? 100 : limit, 500));
        LocalDateTime now = toLocalDateTime(Instant.now(clock));
        List<BiEmbedTokenDO> rows = tokenMapper.selectList(new QueryWrapper<BiEmbedTokenDO>()
                .eq("tenant_id", scopedTenantId)
                .eq("revoked", false)
                .le("expires_at", now)
                .orderByAsc("expires_at")
                .orderByAsc("id")
                .last("LIMIT " + capped));
        int revoked = 0;
        int failed = 0;
        for (BiEmbedTokenDO row : rows == null ? List.<BiEmbedTokenDO>of() : rows) {
            try {
                row.setRevoked(true);
                tokenMapper.updateById(row);
                revoked++;
            } catch (RuntimeException e) {
                failed++;
            }
        }
        return new BiEmbedTokenCleanupResult(rows == null ? 0 : rows.size(), revoked, failed);
    }

    private void consumeInMemory(BiEmbedTicketPayload payload) {
        purgeExpiredInMemoryNonces();
        InMemoryAccessState state = consumedNonces.computeIfAbsent(payload.nonce(),
                ignored -> new InMemoryAccessState(payload.expiresAt()));
        if (!state.tryConsume(Instant.now(clock), maxAccessCount(payload.maxAccessCount()),
                rateLimitPerMinute(payload.rateLimitPerMinute()))) {
            recordAudit(payload, "BI_EMBED_TICKET_REJECTED", null, "REPLAY_REJECTED",
                    "replayed BI embed ticket or rate-limited BI embed ticket", null);
            throw new SecurityException("replayed BI embed ticket or rate-limited BI embed ticket");
        }
        recordAudit(payload, "BI_EMBED_TICKET_CONSUME", null, "CONSUMED", null, null);
    }

    private void purgeExpiredInMemoryNonces() {
        Instant now = Instant.now(clock);
        consumedNonces.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    private void persistToken(String ticket, BiEmbedTicketPayload payload) {
        if (tokenMapper == null) {
            return;
        }
        BiEmbedTokenDO row = new BiEmbedTokenDO();
        row.setTenantId(payload.tenantId());
        row.setTokenHash(tokenHash(ticket));
        row.setResourceType(payload.resourceType());
        row.setResourceId(resourceId(payload.resourceKey()));
        row.setResourceKey(payload.resourceKey());
        row.setUserId(payload.username());
        row.setScopeJson(toJson(payload));
        row.setNonce(payload.nonce());
        row.setExpiresAt(toLocalDateTime(payload.expiresAt()));
        row.setRevoked(false);
        row.setAccessCount(0);
        row.setMaxAccessCount(maxAccessCount(payload.maxAccessCount()));
        row.setRateLimitPerMinute(rateLimitPerMinute(payload.rateLimitPerMinute()));
        row.setRateWindowCount(0);
        row.setCreatedAt(toLocalDateTime(payload.issuedAt()));
        tokenMapper.insert(row);
    }

    private void consumePersistentToken(String ticket, BiEmbedTicketPayload payload, String normalizedOrigin) {
        Instant currentInstant = Instant.now(clock);
        LocalDateTime now = toLocalDateTime(currentInstant);
        LocalDateTime rateWindowStart = toLocalDateTime(currentInstant.minusSeconds(60));
        int updated = tokenMapper.update(null, new UpdateWrapper<BiEmbedTokenDO>()
                .eq("tenant_id", payload.tenantId())
                .eq("token_hash", tokenHash(ticket))
                .eq("revoked", false)
                .gt("expires_at", now)
                .apply("access_count < max_access_count")
                .apply("(rate_window_started_at IS NULL OR rate_window_started_at < {0} "
                        + "OR rate_window_count < rate_limit_per_minute)", rateWindowStart)
                .setSql("revoked = CASE WHEN access_count + 1 >= max_access_count THEN 1 ELSE 0 END")
                .setSql("access_count = access_count + 1")
                .setSql("rate_window_count = CASE WHEN rate_window_started_at IS NULL "
                        + "OR rate_window_started_at < {0} THEN 1 ELSE rate_window_count + 1 END",
                        rateWindowStart)
                .setSql("rate_window_started_at = CASE WHEN rate_window_started_at IS NULL "
                        + "OR rate_window_started_at < {0} THEN {1} ELSE rate_window_started_at END",
                        rateWindowStart, now)
                .set("last_accessed_at", now)
                .set("last_access_origin", normalizedOrigin)
                .setSql("consumed_at = COALESCE(consumed_at, {0})", now)
                .set("consumed_origin", normalizedOrigin));
        if (updated != 1) {
            recordAudit(payload, "BI_EMBED_TICKET_REJECTED", normalizedOrigin, "REPLAY_REJECTED",
                    "replayed BI embed ticket or rate-limited BI embed ticket", ticket);
            throw new SecurityException("replayed BI embed ticket or rate-limited BI embed ticket");
        }
        recordAudit(payload, "BI_EMBED_TICKET_CONSUME", normalizedOrigin, "CONSUMED", null, ticket);
    }

    private void recordAudit(BiEmbedTicketPayload payload,
                             String action,
                             String origin,
                             String status,
                             String reason,
                             String ticket) {
        if (auditLogMapper == null) {
            return;
        }
        try {
            BiAuditLogDO row = new BiAuditLogDO();
            row.setTenantId(payload.tenantId());
            row.setActorId(payload.username());
            row.setActionKey(action);
            row.setResourceType(payload.resourceType());
            row.setResourceId(resourceId(payload.resourceKey()));
            row.setDetailJson(OBJECT_MAPPER.writeValueAsString(Map.of(
                    "resourceKey", payload.resourceKey(),
                    "scope", payload.scope(),
                    "origin", origin == null ? "" : origin,
                    "nonce", payload.nonce(),
                    "status", status,
                    "reason", reason == null ? "" : reason,
                    "tokenHash", ticket == null ? "" : tokenHash(ticket),
                    "allowedDomains", payload.allowedDomains()
            )));
            row.setCreatedAt(toLocalDateTime(Instant.now(clock)));
            auditLogMapper.insert(row);
        } catch (Exception ignored) {
            // Embed access should not fail just because audit storage is temporarily unavailable.
        }
    }

    private int ttl(Integer ttlSeconds) {
        if (ttlSeconds == null) {
            return DEFAULT_TTL_SECONDS;
        }
        if (ttlSeconds < MIN_TTL_SECONDS) {
            return MIN_TTL_SECONDS;
        }
        return Math.min(ttlSeconds, MAX_TTL_SECONDS);
    }

    private Map<String, String> sanitizeFilters(Map<String, String> filters) {
        return sanitizeStringMap(filters, "filter", MAX_FILTERS);
    }

    private Map<String, String> sanitizeParameters(Map<String, String> parameters) {
        return sanitizeStringMap(parameters, "parameter", MAX_PARAMETERS);
    }

    private Map<String, String> sanitizeStringMap(Map<String, String> values, String label, int maxEntries) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        if (values.size() > maxEntries) {
            throw new IllegalArgumentException("too many " + label + "s");
        }
        values.forEach((key, value) -> {
            requireSafeKey(key, label + " key");
            if (value != null && value.length() > 160) {
                throw new IllegalArgumentException(label + " value is too long");
            }
            if (containsControlCharacter(value)) {
                throw new IllegalArgumentException(label + " value contains control characters");
            }
        });
        return Map.copyOf(values);
    }

    private int maxAccessCount(Integer maxAccessCount) {
        if (maxAccessCount == null) {
            return DEFAULT_MAX_ACCESS_COUNT;
        }
        if (maxAccessCount < 1) {
            throw new IllegalArgumentException("maxAccessCount must be at least 1");
        }
        return Math.min(maxAccessCount, MAX_ACCESS_COUNT);
    }

    private int rateLimitPerMinute(Integer rateLimitPerMinute) {
        if (rateLimitPerMinute == null) {
            return DEFAULT_RATE_LIMIT_PER_MINUTE;
        }
        if (rateLimitPerMinute < 1) {
            throw new IllegalArgumentException("rateLimitPerMinute must be at least 1");
        }
        return Math.min(rateLimitPerMinute, MAX_RATE_LIMIT_PER_MINUTE);
    }

    private List<String> sanitizeAllowedDomains(List<String> allowedDomains) {
        if (allowedDomains == null || allowedDomains.isEmpty()) {
            return List.of();
        }
        if (allowedDomains.size() > MAX_ALLOWED_DOMAINS) {
            throw new IllegalArgumentException("too many allowed domains");
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String domain : allowedDomains) {
            result.add(normalizeDomain(domain, "allowedDomain"));
        }
        return List.copyOf(result);
    }

    private String enforceAllowedOrigin(BiEmbedTicketPayload payload, String origin) {
        if (payload.allowedDomains().isEmpty()) {
            if (origin == null || origin.isBlank()) {
                return null;
            }
            try {
                return normalizeDomain(origin, "origin");
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        String normalizedOrigin;
        try {
            normalizedOrigin = normalizeDomain(origin, "origin");
        } catch (IllegalArgumentException e) {
            throw new SecurityException("BI embed ticket origin is not allowed", e);
        }
        if (!payload.allowedDomains().contains(normalizedOrigin)) {
            throw new SecurityException("BI embed ticket origin is not allowed");
        }
        return normalizedOrigin;
    }

    private String normalizeDomain(String value, String name) {
        String text = requireText(value, name).toLowerCase(Locale.ROOT);
        String hostPort = text;
        if (text.contains("://")) {
            try {
                URI uri = URI.create(text);
                if (uri.getHost() == null || uri.getHost().isBlank()) {
                    throw new IllegalArgumentException(name + " must be a host or origin");
                }
                hostPort = uri.getHost().toLowerCase(Locale.ROOT)
                        + (uri.getPort() > 0 ? ":" + uri.getPort() : "");
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(name + " must be a host or origin", e);
            }
        } else if (text.contains("/") || text.contains("@")) {
            throw new IllegalArgumentException(name + " must be a host or origin");
        }
        if (!SAFE_DOMAIN.matcher(hostPort).matches()
                || hostPort.contains("..")
                || hostPort.startsWith(".")
                || hostPort.endsWith(".")) {
            throw new IllegalArgumentException(name + " must be a safe host");
        }
        return hostPort;
    }

    private boolean containsControlCharacter(String value) {
        if (value == null) {
            return false;
        }
        return value.chars().anyMatch(character -> Character.isISOControl((char) character));
    }

    private String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }

    private String requireSafeKey(String value, String name) {
        String text = requireText(value, name);
        if (!SAFE_KEY.matcher(text).matches()) {
            throw new IllegalArgumentException(name + " must be a safe key");
        }
        return text;
    }

    private String toJson(BiEmbedTicketPayload payload) {
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize BI embed ticket", e);
        }
    }

    private BiEmbedTicketPayload fromJson(String json) {
        try {
            return OBJECT_MAPPER.readValue(json, BiEmbedTicketPayload.class);
        } catch (JsonProcessingException e) {
            throw new SecurityException("invalid BI embed ticket");
        }
    }

    private String sign(String payloadPart) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payloadPart.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("failed to sign BI embed ticket", e);
        }
    }

    private String base64Url(String text) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private String tokenHash(String ticket) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(digest.digest(ticket.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("failed to hash BI embed ticket", e);
        }
    }

    private Long resourceId(String resourceKey) {
        if (resourceKey != null && resourceKey.matches("[0-9]{1,18}")) {
            return Long.valueOf(resourceKey);
        }
        return 0L;
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static void validateSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("canvas.bi.embed-secret or canvas.jwt.secret is required");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("BI embed secret must be at least 32 bytes");
        }
    }

    private static final class InMemoryAccessState {
        private final Instant expiresAt;
        private int accessCount;
        private Instant rateWindowStartedAt;
        private int rateWindowCount;

        private InMemoryAccessState(Instant expiresAt) {
            this.expiresAt = expiresAt;
        }

        private Instant expiresAt() {
            return expiresAt;
        }

        private synchronized boolean tryConsume(Instant now, int maxAccessCount, int rateLimitPerMinute) {
            if (!expiresAt.isAfter(now) || accessCount >= maxAccessCount) {
                return false;
            }
            Instant rateWindowStart = now.minusSeconds(60);
            if (rateWindowStartedAt == null || !rateWindowStartedAt.isAfter(rateWindowStart)) {
                rateWindowStartedAt = now;
                rateWindowCount = 0;
            }
            if (rateWindowCount >= rateLimitPerMinute) {
                return false;
            }
            rateWindowCount++;
            accessCount++;
            return true;
        }
    }
}
