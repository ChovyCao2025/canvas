package org.chovy.canvas.domain.bi.embed;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class BiEmbedTicketService {

    private static final int MIN_TTL_SECONDS = 60;
    private static final int DEFAULT_TTL_SECONDS = 600;
    private static final int MAX_TTL_SECONDS = 1800;
    private static final Pattern SAFE_KEY = Pattern.compile("[A-Za-z0-9_-]{1,80}");
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    private final String secret;
    private final Clock clock;

    @Autowired
    public BiEmbedTicketService(@Value("${canvas.bi.embed-secret:${canvas.jwt.secret:}}") String secret) {
        this(secret, Clock.systemUTC());
    }

    public BiEmbedTicketService(String secret, Clock clock) {
        validateSecret(secret);
        this.secret = secret;
        this.clock = clock;
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
        Instant issuedAt = Instant.now(clock);
        Instant expiresAt = issuedAt.plusSeconds(ttl(request.ttlSeconds()));
        BiEmbedTicketPayload payload = new BiEmbedTicketPayload(
                tenantId,
                safeUser,
                resourceType,
                resourceKey,
                scope,
                filters,
                UUID.randomUUID().toString().replace("-", ""),
                issuedAt,
                expiresAt
        );
        String payloadPart = base64Url(toJson(payload));
        String signature = sign(payloadPart);
        String ticket = payloadPart + "." + signature;
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
        if (filters == null || filters.isEmpty()) {
            return Map.of();
        }
        filters.forEach((key, value) -> {
            requireSafeKey(key, "filter key");
            if (value != null && value.length() > 160) {
                throw new IllegalArgumentException("filter value is too long");
            }
        });
        return Map.copyOf(filters);
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

    private static void validateSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("canvas.bi.embed-secret or canvas.jwt.secret is required");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("BI embed secret must be at least 32 bytes");
        }
    }
}
