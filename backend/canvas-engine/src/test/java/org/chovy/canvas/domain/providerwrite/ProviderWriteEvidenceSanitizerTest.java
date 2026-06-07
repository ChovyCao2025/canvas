package org.chovy.canvas.domain.providerwrite;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderWriteEvidenceSanitizerTest {

    @Test
    void recursivelyRedactsCredentialShapedKeysWithoutDroppingSafeEvidence() {
        Map<String, Object> sanitized = ProviderWriteEvidenceSanitizer.sanitizeMap(Map.of(
                "provider", "GOOGLE_ADS",
                "access_token", "raw-access-token",
                "headers", Map.of(
                        "Authorization", "Bearer raw",
                        "requestId", "req-1"),
                "operations", List.of(Map.of(
                        "client_secret", "raw-secret",
                        "mutationType", "UPDATE_KEYWORD_BID"))));

        assertThat(sanitized).containsEntry("provider", "GOOGLE_ADS");
        assertThat(sanitized).containsEntry("access_token", ProviderWriteEvidenceSanitizer.REDACTED);
        Map<?, ?> headers = (Map<?, ?>) sanitized.get("headers");
        assertThat(headers.get("Authorization")).isEqualTo(ProviderWriteEvidenceSanitizer.REDACTED);
        assertThat(headers.get("requestId")).isEqualTo("req-1");
        Map<?, ?> operation = (Map<?, ?>) ((List<?>) sanitized.get("operations")).get(0);
        assertThat(operation.get("client_secret")).isEqualTo(ProviderWriteEvidenceSanitizer.REDACTED);
        assertThat(operation.get("mutationType")).isEqualTo("UPDATE_KEYWORD_BID");
    }

    @Test
    void returnsEmptyMapForNullEvidence() {
        assertThat(ProviderWriteEvidenceSanitizer.sanitizeMap(null)).isEmpty();
    }
}
