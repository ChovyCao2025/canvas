package org.chovy.canvas.domain.providerwrite;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class ProviderWriteSandboxSupport {

    private ProviderWriteSandboxSupport() {
    }

    public static boolean supportsSandboxProvider(String provider) {
        String normalized = normalize(provider);
        return "SANDBOX".equals(normalized)
                || normalized.endsWith("_SANDBOX")
                || normalized.startsWith("SANDBOX_");
    }

    public static String operationId(String domain,
                                     String provider,
                                     String mutationType,
                                     String idempotencyKey,
                                     boolean dryRun) {
        String material = String.join("|",
                defaultString(domain),
                normalize(provider),
                normalize(mutationType),
                defaultString(idempotencyKey),
                dryRun ? "dry-run" : "apply");
        return "sandbox-" + defaultString(domain) + "-" + (dryRun ? "validate-" : "apply-")
                + sha256(material).substring(0, 16);
    }

    public static Map<String, Object> response(String domain,
                                               String provider,
                                               String mutationType,
                                               String entityType,
                                               String externalEntityId,
                                               String idempotencyKey,
                                               boolean dryRun,
                                               boolean partialFailure,
                                               Map<String, Object> payload,
                                               Map<String, Object> metadata) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("adapter", "sandbox");
        response.put("domain", domain);
        response.put("provider", provider);
        response.put("mutationType", mutationType);
        response.put("entityType", entityType);
        response.put("externalEntityId", defaultString(externalEntityId));
        response.put("idempotencyKey", defaultString(idempotencyKey));
        response.put("dryRun", dryRun);
        response.put("partialFailure", partialFailure);
        response.put("validated", true);
        response.put("applied", !dryRun);
        response.put("payloadHash", sha256(String.valueOf(payload == null ? Map.of() : payload)));
        response.put("metadata", ProviderWriteEvidenceSanitizer.sanitizeMap(metadata));
        response.put("access_token", "sandbox-token-never-persist");
        return ProviderWriteEvidenceSanitizer.sanitizeMap(response);
    }

    private static String normalize(String value) {
        return defaultString(value).toUpperCase(Locale.ROOT);
    }

    private static String defaultString(String value) {
        return value == null ? "" : value.trim();
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not hash provider write sandbox evidence", ex);
        }
    }
}
