package org.chovy.canvas.platform.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PublicIngressCatalog {

    public Map<String, Object> publicMarketingForm(String publicKey) {
        requireText(publicKey, "publicKey is required");
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("publicKey", publicKey.trim());
        form.put("name", "Lead capture");
        form.put("formName", "Public form " + publicKey.trim());
        form.put("fieldSchema", List.of("email", "company", "message"));
        form.put("successMessage", "Thanks, we received your request.");
        form.put("active", true);
        return form;
    }

    public Map<String, Object> submitMarketingForm(String publicKey, Map<String, Object> payload,
                                                   Map<String, String> headers) {
        requireText(publicKey, "publicKey is required");
        Map<String, Object> safePayload = payload == null ? Map.of() : payload;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", true);
        result.put("publicKey", publicKey.trim());
        result.put("submissionId", stableId("submission", publicKey, String.valueOf(safePayload)));
        result.put("anonymousId", safePayload.get("anonymousId"));
        result.put("idempotencyKey", safePayload.get("idempotencyKey"));
        result.put("consentStatus", safePayload.get("consentStatus"));
        result.put("receivedAt", Instant.EPOCH.toString());
        result.put("userAgent", header(headers, "User-Agent"));
        return result;
    }

    public Map<String, Object> submitMarketingForm(String publicKey, Map<String, Object> response,
                                                   Map<String, Object> utm, String anonymousId,
                                                   String idempotencyKey, String consentChannel,
                                                   String consentStatus, String userAgent, String ipHash) {
        requireText(publicKey, "publicKey is required");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", true);
        result.put("publicKey", publicKey.trim());
        result.put("submissionId", stableId("submission", publicKey, String.valueOf(response)));
        result.put("response", response == null ? Map.of() : response);
        result.put("utm", utm == null ? Map.of() : utm);
        result.put("anonymousId", anonymousId);
        result.put("idempotencyKey", idempotencyKey);
        result.put("consentChannel", consentChannel);
        result.put("consentStatus", consentStatus);
        result.put("userAgent", userAgent);
        result.put("ipHash", ipHash);
        return result;
    }

    public String verifyWhatsApp(Long tenantId, String mode, String verifyToken, String challenge) {
        requireTenant(tenantId);
        requireText(challenge, "hub.challenge is required");
        Map<String, Object> verification = new LinkedHashMap<>();
        verification.put("tenantId", tenantId);
        verification.put("mode", mode);
        verification.put("verifyTokenPresent", verifyToken != null && !verifyToken.isBlank());
        return challenge;
    }

    public List<Map<String, Object>> receiveWhatsApp(Long tenantId, String signature, String rawBody) {
        requireTenant(tenantId);
        requireJson(rawBody, "whatsapp webhook payload must be JSON");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tenantId", tenantId);
        result.put("provider", "WHATSAPP");
        result.put("channel", "WHATSAPP");
        result.put("source", "whatsapp-webhook");
        result.put("accepted", true);
        result.put("signaturePresent", signature != null && !signature.isBlank());
        result.put("messageCount", rawBody.contains("entry") ? 1 : 0);
        result.put("ingressId", stableId("whatsapp", tenantId.toString(), rawBody));
        return List.of(result);
    }

    public Map<String, Object> receiveAssetUploadCallback(Long tenantId, String provider, String timestamp,
                                                          String signature, String rawBody) {
        requireTenant(tenantId);
        requireText(provider, "provider is required");
        requireJson(rawBody, "asset upload callback payload must be JSON");
        String payloadProvider = jsonString(rawBody, "provider");
        if (payloadProvider != null && !payloadProvider.equalsIgnoreCase(provider.trim())) {
            throw new IllegalArgumentException("asset upload provider does not match route");
        }
        Map<String, Object> result = callback("asset-upload", tenantId, rawBody);
        result.put("provider", provider.trim());
        result.put("uploadToken", valueOrDefault(jsonString(rawBody, "uploadToken"), "upload-" + tenantId));
        result.put("status", valueOrDefault(jsonString(rawBody, "status"), "RECEIVED"));
        result.put("timestamp", timestamp);
        result.put("signaturePresent", signature != null && !signature.isBlank());
        return result;
    }

    public Map<String, Object> receiveMonitoringWebhook(Long tenantId, String sourceKey, String timestamp,
                                                        String signature, String rawBody) {
        requireTenant(tenantId);
        requireText(sourceKey, "sourceKey is required");
        requireJson(rawBody, "marketing monitoring webhook payload must be JSON");
        Map<String, Object> result = callback("monitoring", tenantId, rawBody);
        result.put("sourceKey", sourceKey.trim());
        result.put("timestamp", timestamp);
        result.put("signaturePresent", signature != null && !signature.isBlank());
        return result;
    }

    private static Map<String, Object> callback(String kind, Long tenantId, String rawBody) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tenantId", tenantId);
        result.put("kind", kind);
        result.put("accepted", true);
        result.put("rawSize", rawBody.length());
        result.put("callbackId", stableId(kind, tenantId.toString(), rawBody));
        return result;
    }

    private static String header(Map<String, String> headers, String name) {
        if (headers == null || headers.isEmpty()) {
            return null;
        }
        return headers.get(name);
    }

    private static void requireTenant(Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("tenantId is required");
        }
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireJson(String rawBody, String message) {
        requireText(rawBody, message);
        String trimmed = rawBody.trim();
        if (!(trimmed.startsWith("{") && trimmed.endsWith("}")) && !(trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            throw new IllegalArgumentException(message);
        }
    }

    private static String jsonString(String rawBody, String field) {
        String marker = "\"" + field + "\"";
        int fieldIndex = rawBody.indexOf(marker);
        if (fieldIndex < 0) {
            return null;
        }
        int colon = rawBody.indexOf(':', fieldIndex + marker.length());
        int startQuote = colon < 0 ? -1 : rawBody.indexOf('"', colon + 1);
        int endQuote = startQuote < 0 ? -1 : rawBody.indexOf('"', startQuote + 1);
        if (endQuote < 0) {
            return null;
        }
        String value = rawBody.substring(startQuote + 1, endQuote);
        return value.isBlank() ? null : value;
    }

    private static String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static String stableId(String prefix, String left, String right) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((left + ":" + right).getBytes(StandardCharsets.UTF_8));
            return prefix + "-" + HexFormat.of().formatHex(bytes).substring(0, 12);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is unavailable", ex);
        }
    }
}
