package org.chovy.canvas.engine.channel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class ChannelDedupeService {

    private final Repository repository;
    private final JsonMapper jsonMapper;

    public ChannelDedupeService(Repository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.jsonMapper = JsonMapper.builder()
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                .build();
    }

    public Decision reserve(Long tenantId,
                            String dedupeGroup,
                            String contentHash,
                            String channel,
                            String userId,
                            Duration ttl) {
        boolean reserved = repository.reserve(
                ChannelConnectorRegistry.tenant(tenantId),
                normalizeGroup(dedupeGroup),
                contentHash,
                ChannelConnectorRegistry.normalize(channel),
                normalizeUser(userId),
                ttl == null ? Duration.ofHours(24) : ttl);
        return new Decision(reserved ? "RESERVED" : "DUPLICATE", contentHash);
    }

    public Decision reservePayload(Long tenantId,
                                   String dedupeGroup,
                                   String channel,
                                   String userId,
                                   String templateId,
                                   Map<String, Object> payload,
                                   Duration ttl) {
        return reserve(tenantId, dedupeGroup, hashPayload(channel, templateId, payload), channel, userId, ttl);
    }

    public String hashPayload(String channel, String templateId, Map<String, Object> payload) {
        Map<String, Object> canonical = new LinkedHashMap<>();
        canonical.put("channel", ChannelConnectorRegistry.normalize(channel));
        canonical.put("templateId", templateId == null ? "" : templateId);
        canonical.put("payload", payload == null ? Map.of() : payload);
        try {
            return sha256(jsonMapper.writeValueAsString(canonical));
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Unable to hash channel payload", ex);
        }
    }

    private static String normalizeGroup(String value) {
        return value == null || value.isBlank() ? "default" : value.trim();
    }

    private static String normalizeUser(String value) {
        return value == null || value.isBlank() ? "anonymous" : value.trim();
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    public interface Repository {
        boolean reserve(Long tenantId, String dedupeGroup, String contentHash, String channel, String userId, Duration ttl);
    }

    public record Decision(String status, String contentHash) {
    }
}
